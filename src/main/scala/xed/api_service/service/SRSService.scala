package xed.api_service.service

import akka.actor.ActorRef
import com.fasterxml.jackson.databind.node.TextNode
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.exception._
import xed.api_service.domain.metric.{CardReport, LineData}
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.domain.{Deck, ReviewInfo, SRSCard, SRSSource, SRSStatus}
import xed.api_service.repository.SRSRepository
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.{Implicits, JsonUtils, TimeUtils, Utils, ZConfig}
import xed.chatbot.domain.leaderboard.LeaderBoardItem
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SignedInUser, UserProfileService}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

trait SRSService extends Logging {

  def add(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo]

  def multiAdd(user: SignedInUser, source: String, cardIds: Seq[String]): Future[Map[String,ReviewInfo]]

  def ignore(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo]

  def makeDone(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo]

  def learnAgain(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo]

  def review(user: SignedInUser, source: String, request: ReviewRequest) : Future[ReviewInfo]

  def delete(user: SignedInUser, source: String, cardId: String): Future[Boolean]

  def multiDelete(username: String, source: String, cardId: Seq[String]): Future[Int]

  def getTotalDueCardBySource(user: SignedInUser): Future[Map[String,Long]]

  def getTotalDueCardBySource(user: SignedInUser, source: String): Future[Long]

  def getDueCardIds(user: SignedInUser, source: String): Future[Seq[String]]

  def searchDueCards(user: SignedInUser, source: String, searchRequest: SearchRequest) : Future[PageResult[SRSCard]]

  def searchLearningCards(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[SRSCard]]

  def searchDoneCards(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[SRSCard]]

  def search(searchRequest: SearchRequest,  source: String): Future[PageResult[SRSCard]]

  def getDueDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]]

  def getLearningDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]]

  def getDoneDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]]

  def getReviewCards(user: SignedInUser, source: String, cardIds: Seq[String]) : Future[Seq[SRSCard]]

  def getReviewInfo(user: SignedInUser, source: String, cardIds: Seq[String]) : Future[Map[String,ReviewInfo]]

  def getCardReport(user: SignedInUser, request: GetReportRequest): Future[Map[String, LineData]]

  def getCardReportV2(user: SignedInUser, request: GetReportRequest): Future[CardReport]

  def getTopByNewCard(request: GetTopByNewCardRequest): Future[Seq[LeaderBoardItem]]
}

case class SRSServiceImpl(repository: SRSRepository,
                          cardService: CardService,
                          deckService: DeckService,
                          historyService: HistoryService,
                          profileService: UserProfileService,
                          analytics: Analytics,
                          eventPublisher: ActorRef) extends SRSService {

  private val repetitionScales = ZConfig.getIntList("srs.repetition_scales", Seq(1, 1, 3, 5, 7, 10, 14, 20, 35, 70, 85, 97))
  private val maxThreshold = ZConfig.getInt("srs.repetition_threshold", 6)
  private val testerIds = ZConfig.getStringList("srs.testers", List.empty).toSet

  protected def calcDueDate(username: String,
                            memorizingLevel: Int,
                            isNeedReviewNow: Boolean = true): (Long, Long) = {

    val space = if (memorizingLevel < repetitionScales.size) {
      val index = if (memorizingLevel < 0) 0 else memorizingLevel
      repetitionScales(index)
    } else {
      (115 / (1 + 2192 * math.exp(-0.79 * memorizingLevel))).toInt
    }

    val (time, _) = if (isNeedReviewNow || testerIds.contains(username))
      TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())
    else
      TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis() + 1.days.toMillis)

    (time, time + space.days.toMillis)
  }

  /**
   * Create a new SRS Model on this card for this user.
   *
   * @param user
   * @param cardId
   * @return
   */
  protected def createLearningSRSInfo(user: SignedInUser,
                                      source: String,
                                      deckId: String,
                                      cardId: String) = {
    val memorizingLevel = 0
    val needReviewNow = source.equalsIgnoreCase(SRSSource.FLASHCARD)
    val (date, _) = calcDueDate(
      user.username,
      memorizingLevel,
      needReviewNow
    )
    ReviewInfo(
      id = SRSRepository.buildReviewId(user.username, source, cardId),
      source = source,
      deckId = Some(deckId),
      cardId = Some(cardId),
      username = Some(user.username),
      status = Some(SRSStatus.Learning),
      memorizingLevel = Some(memorizingLevel),
      startDate = Some(date),
      dueDate = Some(date),
      lastReviewTime = Some(System.currentTimeMillis()),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }


  override def add(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo] = {
    for {
      card <- cardService.getCard(cardId)
      model <- repository.getReviewInfo(user.username, source, cardId).map({
        case Some(model) => model.asLearningModel()
        case _ => createLearningSRSInfo(user, source, card.deckId.get, cardId)
      })
      r <- repository.insert(model, false)
    } yield r match {
      case Some(_) =>
        publishAddedEvent(user.username,
          source,
          model.id,
          cardId)
        model
      case _ => throw InternalError(Some("Can't add this card to Review system."))
    }
  }

  override def multiAdd(user: SignedInUser, source: String, cardIds: Seq[String]): Future[Map[String, ReviewInfo]] = {
    for {
      cards <- cardService.getCardAsMap(cardIds)
      validCardIds = cards.keys.toSeq
      oldModels <- repository.getReviewInfos(user.username, source, validCardIds).map(_.map(x => x._2.cardId.get -> x._2))
      r <- {
        val models = validCardIds.map(cardId => {
          val deckId = cards.get(cardId).flatMap(_.deckId).get
          oldModels.get(cardId).fold(createLearningSRSInfo(user, source, deckId, cardId))(x => x)
        })
        repository.multiInsert(models, false)
      }
    } yield r.nonEmpty match {
      case true =>
        mpublishAddedEvent(user.username, source, r)
        r.map(x => x.cardId.get -> x).toMap
      case _ => throw InternalError(Some("Can't add these cards to Review system."))
    }
  }

  override def makeDone(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo] = {
    for {
      card <- cardService.getCard(cardId)
      model <- repository.getReviewInfo(user.username, source, cardId).map({
        case Some(model) if model.isDoneCard() => throw InternalError(Some("This card is a done card already."))
        case Some(model) if !model.isDoneCard() => model.asDoneModel()
        case _ => createLearningSRSInfo(user, source, card.deckId.get, cardId).asDoneModel()
      })

      r <- repository.insert(model, false)
    } yield r match {
      case Some(_) =>
        publishUpdatedEvent(user.username, source, model.id)
        model
      case _ => throw InternalError(Some("Can't make this card as DONE."))
    }
  }

  override def ignore(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo] = {
    for {
      card <- cardService.getCard(cardId)
      model <- repository.getReviewInfo(user.username, source, cardId).map({
        case Some(model) if model.isDoneCard() => throw InternalError(Some("This card is a done card."))
        case Some(model) if !model.isDoneCard() => model.asIgnoreModel()
        case _ =>
          createLearningSRSInfo(
            user,
            source,
            card.deckId.get,
            cardId
          ).asIgnoreModel()
      })

      r <- repository.insert(model, false)
    } yield r match {
      case Some(_) =>
        publishUpdatedEvent(user.username, source, model.id)
        model
      case _ => throw InternalError(Some("Can't stop review this card."))
    }
  }

  override def learnAgain(user: SignedInUser, source: String, cardId: String): Future[ReviewInfo] = {
    for {
      card <- cardService.getCard(cardId)
      model = createLearningSRSInfo(user, source, card.deckId.get, cardId)
      r <- repository.insert(model, false)
    } yield r match {
      case Some(_) =>
        publishUpdatedEvent(user.username, source, model.id)
        model
      case _ => throw InternalError()
    }
  }

  override def review(user: SignedInUser, source: String, request: ReviewRequest): Future[ReviewInfo] = {

    def recordHistory(status: Boolean, request: ReviewRequest, srsInfo: ReviewInfo) = {
      analytics.log(Operation.REVIEW,
        user.userProfile,
        Map(
          "username" -> user.username,
          "model_id" -> srsInfo.id,
          "card_id" -> srsInfo.cardId,
          "source" -> source,
          "is_pass" -> status
        ))


      val fn = if (status)
        historyService.recordSuccess(user, request, Some(srsInfo))
      else
        historyService.recordError(user, request, Some(srsInfo))

      fn.transform({
        case Return(r) => Future.True
        case Throw(e) =>
          error(s"recordHistory", e)
          Future.False
      })

    }

    def evaluate(srsInfo: ReviewInfo, request: ReviewRequest) = {
      val level = request.answer match {
        case x if x <= 0 => 0
        case x if x == 1 => srsInfo.memorizingLevel.getOrElse(0)
        case x => srsInfo.memorizingLevel.map(_ + 1).getOrElse(1)
      }
      val (date, dueDate) = calcDueDate(user.username, level)
      val newModel = srsInfo.copy(
        memorizingLevel = Some(level),
        startDate = Some(date),
        dueDate = Some(dueDate),
        lastReviewTime = Some(System.currentTimeMillis()),
        updatedTime = Some(System.currentTimeMillis())
      )

      if (level < maxThreshold) newModel else newModel.asDoneModel()
    }

    val fn = for {
      model <- repository.getReviewInfo(user.username, source, request.cardId)
        .map(Utils.throwIfNotExist(_, Some(s"this card haven't add to SRS yet.")))
      r <- if (model.reviewRequired()) {
        val newModel = evaluate(model, request)
        repository
          .update(newModel)
          .map(x => if (x.count > 0) Some(newModel) else None)
      } else Future.value(Some(model))
      _ <- if (model.reviewRequired()) recordHistory(r.isDefined, request, model) else Future.True
    } yield r match {
      case Some(reviewInfo) =>
        publishUpdatedEvent(user.username, source, model.id)
        reviewInfo
      case _ => throw InternalError(Some("Review failure."))
    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        historyService
          .recordError(user, request, None)
          .flatMap(_ => Future.exception(e))

    })
  }

  override def delete(user: SignedInUser, source: String, cardId: String): Future[Boolean] = {
    val reviewId = SRSRepository.buildReviewId(user.username, source, cardId)
    repository.deleteReviewInfo(user.username, source, cardId).map(r => {
      if (r)
        publishRemovedEvent(user.username, source, reviewId)
      r
    })
  }

  override def multiDelete(username: String, source: String, cardIds: Seq[String]): Future[Int] = {
    repository.deleteReviewInfos(username, source, cardIds).map(r => {
      cardIds.foreach(cardId => {
        val reviewId = SRSRepository.buildReviewId(username, source, cardId)
        publishRemovedEvent(username, source, reviewId)
      })
      r
    })
  }

  def getTotalDueCardBySource(user: SignedInUser): Future[Map[String, Long]] = {
    repository.getTotalDueCardDetail(user.username)
  }

  def getTotalDueCardBySource(user: SignedInUser, source: String): Future[Long] = {

    repository.getTotalDueCardBySource(user.username,source)
  }

  override def getDueCardIds(user: SignedInUser, source: String): Future[Seq[String]] = Implicits.async {

    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())

    val newRequest = SearchRequest()
      .removeField("username")
      .removeRangeField("due_date")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(RangeQuery("due_date", None, false, Some(time), true))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))
    val cardIds = ListBuffer.empty[String]

    repository.foreach(newRequest, 20, hits => {
      val ids = hits.map(x => JsonUtils.fromJson[ReviewInfo](x.getSourceAsString))
        .map(_.cardId)
        .filter(_.isDefined)
        .map(_.get)

      cardIds.append(ids: _*)
    }).sync()

    cardIds
  }

  override def searchDueCards(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[SRSCard]] = {

    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())

    val newRequest = searchRequest
      .removeField("username")
      .removeRangeField("due_date")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(RangeQuery("due_date", None, false, Some(time), true))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))
    for {
      r <- repository.genericSearch(newRequest).flatMap(enhanceCardDetail)
    } yield r
  }

  override def searchLearningCards(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[SRSCard]] = {

    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())
    val newRequest = searchRequest
      .removeTermsField("username")
      .removeRangeField("due_date")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(RangeQuery("due_date", Some(time), false, None, false))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))
    for {
      r <- repository.genericSearch(newRequest).flatMap(enhanceCardDetail)
    } yield r
  }

  override def searchDoneCards(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[SRSCard]] = {
    val newRequest = searchRequest
      .removeTermsField("username")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Done))))
      .addIfNotExist(SortQuery("last_review_time", SortQuery.ORDER_DESC))
    repository.genericSearch(newRequest).flatMap(enhanceCardDetail)
  }

  override def search(searchRequest: SearchRequest, source: String): Future[PageResult[SRSCard]] = {
    val newRequest = searchRequest
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))
    repository.genericSearch(newRequest).flatMap(enhanceCardDetail)
  }

  private def searchAndGetAllDecks(user: SignedInUser, searchRequest: SearchRequest): Future[PageResult[Deck]] = {
    val newRequest = searchRequest.copy(from = 0, size = 1)
    for {
      total <- repository.genericSearch(newRequest).map(_.total)
      deckMapCardIds <- repository.getDeckIds(newRequest.copy(size = math.max(total.toInt, 1)))
      decks <- deckService.getDeckAsMap(user, deckMapCardIds.keySet.toSeq)
        .flatMap(enhanceOwnerDetails)
        .map(decks => {
          decks.values.map(deck => {
            val deckId = deck.id
            val cards = mutable.Set(deckMapCardIds.get(deckId).getOrElse(Seq.empty): _*)
            val cardIds = deck.cards
              .getOrElse(ListBuffer.empty)
              .filter(x => cards.contains(x))
            cardIds.foreach(x => cards.remove(x))
            cardIds.appendAll(cards)
            deck.copy(cards = Option(ListBuffer[String](cardIds: _*)))
          }).toSeq
        })
    } yield {
      PageResult[Deck](decks.size, decks)
    }
  }

  override def getDueDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]] = {
    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())

    val newRequest = searchRequest
      .removeField("username")
      .removeField("due_date")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(RangeQuery("due_date", None, false, Some(time), true))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))

    searchAndGetAllDecks(user, newRequest)
  }

  override def getLearningDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]] = {
    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())
    val newRequest = searchRequest
      .removeField("username")
      .removeRangeField("due_date")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Learning))))
      .addIfNotExist(RangeQuery("due_date", Some(time), false, None, false))
      .addIfNotExist(SortQuery("due_date", SortQuery.ORDER_ASC))

    searchAndGetAllDecks(user, newRequest)
  }

  override def getDoneDecks(user: SignedInUser, source: String, searchRequest: SearchRequest): Future[PageResult[Deck]] = {
    val newRequest = searchRequest
      .removeField("username")
      .removeField("source")
      .addIfNotExist(TermsQuery("source", ListBuffer(TextNode.valueOf(source))))
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(SRSStatus.Done))))
      .addIfNotExist(SortQuery("last_review_time", SortQuery.ORDER_DESC))

    searchAndGetAllDecks(user, newRequest)
  }

  def multiGetReviewInfo(username: String, source: String, cardIds: Seq[String]): Future[Map[String, ReviewInfo]] = {
    for {
      srsInfoMap <- repository.getReviewInfos(username, source, cardIds)
        .map(_.map(x => x._2.cardId.get -> x._2))
      r = cardIds
        .filter(srsInfoMap.contains)
        .map(x => srsInfoMap.get(x).get)
        .map(x => x.cardId.get -> x)
        .toMap
    } yield r
  }

  override def getReviewInfo(user: SignedInUser, source: String, cardIds: Seq[String]): Future[Map[String, ReviewInfo]] = {
    for {
      srsInfoMap <- repository.getReviewInfos(user.username, source, cardIds)
        .map(_.map(x => x._2.cardId.get -> x._2))
      r = cardIds
        .filter(srsInfoMap.contains)
        .map(x => srsInfoMap.get(x).get)
        .map(x => x.cardId.get -> x)
        .toMap
    } yield r
  }

  override def getReviewCards(user: SignedInUser, source: String, cardIds: Seq[String]): Future[Seq[SRSCard]] = {
    for {
      cardMap <- cardService.getCardAsMap(cardIds)
      srsInfoMap <- repository.getReviewInfos(user.username, source, cardMap.keys.toSeq)
        .map(_.map(x => x._2.cardId.get -> x._2))
    } yield {
      cardIds.map(srsInfoMap.get)
        .filter(_.isDefined)
        .map(_.get)
        .map(srsInfo => {
          val cardId = srsInfo.cardId.get
          SRSCard(cardId,
            card = cardMap.get(cardId).get,
            model = Some(srsInfo)
          )
        })
    }
  }

  private def enhanceCardDetail(modelResult: PageResult[ReviewInfo]) = {
    val cardIds = modelResult.records.map(_.cardId).filter(_.isDefined).map(_.get)
    cardService.getCardAsMap(cardIds).map(cardMap => {
      val srsCards = modelResult.records.map(model => {
        val cardId = model.cardId.get
        SRSCard(cardId, cardMap.get(cardId).getOrElse(null), Some(model))
      })
      modelResult.copy(records = srsCards)
    })
  }

  private def enhanceOwnerDetails(decks: Map[String, Deck]): Future[Map[String, Deck]] = {
    val userNames = decks.flatMap(_._2.username).toSeq

    val injectFn = (requests: Map[String, Deck], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        if (request._2.username.isDefined) {
          request._2.ownerDetail = users.get(request._2.username.get)
        }
      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(decks, shortProfiles)
    } yield r
  }


  override def getCardReport(user: SignedInUser, request: GetReportRequest): Future[Map[String, LineData]] = {
    repository.getCardReport(user.username,
      request.interval,
      request.fromTime,
      request.toTime).map(reportMap => {
      val r = new mutable.HashMap[String, LineData]()
      r.put(SRSStatus.Learning, LineData(SRSStatus.Learning, 0, Seq.empty))
      r.put(SRSStatus.Done, LineData(SRSStatus.Done, 0, Seq.empty))
      r.put(SRSStatus.Ignored, LineData(SRSStatus.Ignored, 0, Seq.empty))
      reportMap.foreach(e => r.put(e._1, e._2))
      r.toMap
    })
  }

  override def getCardReportV2(user: SignedInUser, request: GetReportRequest): Future[CardReport] = {
    for {
      newCardCount <- repository.getNewCardCount(user.username, request.fromTime, request.toTime)
      reportMap <- repository.getCardReportV2(
        user.username,
        request.fromTime,
        request.toTime
      )
    } yield {
      CardReport(
        newCardCount = newCardCount,
        learningCardCount = reportMap.get(SRSStatus.Learning).getOrElse(0L),
        completedCardCount = reportMap.get(SRSStatus.Done).getOrElse(0L),
        ignoredCardCount = reportMap.get(SRSStatus.Ignored).getOrElse(0L)
      )
    }
  }

  override def getTopByNewCard(request: GetTopByNewCardRequest): Future[Seq[LeaderBoardItem]] = {
    repository.getTopByNewCard(
      request.fromTime,
      request.toTime,
      100)
  }

  private def mpublishAddedEvent(username: String,
                                  source: String,
                                  reviewInfos: Seq[ReviewInfo]): Unit = {
    reviewInfos.foreach(x => {
      publishAddedEvent(username,
        source,
        x.id,
        x.cardId.getOrElse(""))
    })
  }

  protected def publishAddedEvent(username: String,
                                  source: String,
                                  reviewId: String,
                                  cardId: String): Unit = {
    analytics.log(Operation.ADD_TO_REVIEW,
      None,
      Map(
        "username" -> username,
        "model_id" -> reviewId,
        "card_id" -> cardId,
        "source" -> source
      ))
    eventPublisher ! ReviewAddedMessage(source, username, reviewId)
  }

  protected def publishUpdatedEvent(username: String, source: String, reviewId: String): Unit = {
    eventPublisher ! ReviewUpdatedMessage(source, username, reviewId)
  }

  protected def publishRemovedEvent(username: String, source: String, reviewId: String): Unit = {
    eventPublisher ! ReviewRemovedMessage(source, username, reviewId)
  }
}



