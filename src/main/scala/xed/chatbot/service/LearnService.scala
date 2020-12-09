package xed.chatbot.service

import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.api_service.domain.course.{LearnCard, UserLearningInfo}
import xed.api_service.domain.exception.InternalError
import xed.api_service.domain.{Card, Deck, SRSSource}
import xed.api_service.service.course.{CourseService, JourneyService}
import xed.api_service.service.{ CardService, DeckService, SRSService}
import xed.api_service.util.Utils
import xed.chatbot.domain.CourseProgress
import xed.chatbot.repository.CourseLearningRepository
import xed.profiler.Profiler
import xed.userprofile.{SignedInUser, UserProfileService}

import scala.collection.mutable.ListBuffer

/**
 * @author andy - andy
 * @since 12/15/19
 **/
trait LearnService {

  protected lazy val clazz = getClass.getSimpleName

  def getStatistics(email: String) : Future[Map[String,Any]]

  def resetLearningCourse(email: String,  isFullReset: Boolean) : Future[Boolean]

  def learnCard(user: SignedInUser, learnCard: LearnCard): Future[Boolean]

  def ignoreCard(user: SignedInUser, learnCard: LearnCard): Future[Boolean]

  def _learnCard(user: SignedInUser,
                 sourceId: String,
                 sectionId: String,
                 topicId: String,
                 cardId: String): Future[Boolean]

  def _ignoreCard(user: SignedInUser,
                  sourceId: String,
                  sectionId: String,
                  topicId: String,
                  cardId: String): Future[Boolean]

  def addLearningInfo(learningInfo: UserLearningInfo) : Future[Boolean]

  def resetLearningInfo(user: SignedInUser): Future[Option[ UserLearningInfo]]

  def getLearningInfo(user: SignedInUser): Future[Option[UserLearningInfo]]

  def updateLearningCards(user: SignedInUser, learningInfo: UserLearningInfo): Future[UserLearningInfo]

  def getOrCreateLearningInfo(user: SignedInUser): Future[UserLearningInfo]

  def changeCourse(user: SignedInUser, courseId: String): Future[Option[UserLearningInfo]]

  def isCourseCompleted(recipient: SignedInUser, id: String): Future[Boolean]

  def courseCompletionCheck(user: SignedInUser,
                            learningInfo: UserLearningInfo): Future[Option[String]]



  def checkCompletedCourse(user: SignedInUser, courseId: String, cardIds: Seq[String]): Future[Option[String]]


  def completionCheck(user: SignedInUser,
                      learningInfo: UserLearningInfo,
                      courseId: String,
                      sessionId: String,
                      topicId: String): Future[CourseProgress]

  def isNewCourse(user: SignedInUser, courseId: String ): Future[Boolean]

  def isNewTopic(recipient: SignedInUser, topicId: String) : Future[Boolean]

  def isNewSection(recipient: SignedInUser, sectionId: String) : Future[Boolean]

  def markLearning(user: SignedInUser,
                   courseId: Option[String],
                   sectionId: Option[String],
                   topicId: Option[String]): Future[Boolean]
}

case class LearnServiceImpl(repository: CourseLearningRepository,
                            srsService: SRSService,
                            courseService: CourseService,
                            journeyService: JourneyService,
                            deckService: DeckService,
                            cardService: CardService,
                            profileService: UserProfileService) extends LearnService with Logging {


  override def getStatistics(email: String): Future[Map[String, Any]] = {

    for {
      profile <- profileService.getProfileByEmail(email)
        .map(Utils.throwIfNotExist(_, Some(s"The email: $email doesn't exist.")))
      username = profile.username

      maybeLearningInfo = repository.getLearningInfo(username)

      stats = maybeLearningInfo.map(info => {
        val courseId = info.courseId
        val cardIds = info.allLearningCardIds()
        val completedCardIds = if (courseId.isDefined)
          repository.getDoneCardIds(username, courseId.get)
        else
          Seq.empty

        Map(
          "completed" -> Map(
            "total" -> completedCardIds.size,
            "card_ids" -> completedCardIds
          ),
          "learning" -> Map(
            "total" -> cardIds.size,
            "current_card" -> info.getLearningCardId(),
            "card_ids" -> cardIds
          ),
          "user_detail" -> profile
        )
      })
    } yield {
      Map(
        "learning_started" -> maybeLearningInfo.isDefined,
        "statistics" -> stats
      )
    }
  }

  override def resetLearningCourse(email: String, isFullReset: Boolean = false): Future[Boolean] = {

    Future.False
  }

  override def learnCard(user: SignedInUser, learnCard: LearnCard): Future[Boolean] = {
    for {
      card <- cardService.getCard(learnCard.cardId)
      addedReview <- _learnCard(user,
        learnCard.courseId,
        learnCard.sectionId,
        learnCard.topicId,
        card.id
      )
    } yield {
      addedReview
    }
  }

  override def ignoreCard(user: SignedInUser, learnCard: LearnCard): Future[Boolean] = {
    for {
      card <- cardService.getCard(learnCard.cardId)

      addedReview <- _ignoreCard(user,
        learnCard.courseId,
        learnCard.sectionId,
        learnCard.topicId,
        card.id
      )
    } yield {
      addedReview
    }
  }

  override def _learnCard(user: SignedInUser,
                          courseId: String,
                          sectionId: String,
                          topicId: String,
                          cardId: String): Future[Boolean] = {
    srsService.add(user,SRSSource.BOT, cardId).map(reviewInfo => {
      repository.addLearntCardId(
        user.username,
        courseId,
        sectionId,
        topicId,
        cardId)
      true
    })
  }

  override def _ignoreCard(user: SignedInUser,
                           courseId: String,
                           sectionId: String,
                           topicId: String,
                           cardId: String): Future[Boolean] = {
    srsService.ignore(user, SRSSource.BOT, cardId).map(reviewInfo => {
      repository.addLearntCardId(
        user.username,
        courseId,
        sectionId,
        topicId,
        cardId)
      true
    })
  }


  override def isCourseCompleted(recipient: SignedInUser, courseId: String): Future[Boolean] = Future {
    repository.isCompletedCourse(recipient.username, courseId)
  }


  def courseCompletionCheck(user: SignedInUser, learningInfo: UserLearningInfo): Future[Option[String]] = {
    if (learningInfo.courseId.isDefined) {
      checkCompletedCourse(user,
        learningInfo.courseId.get,
        learningInfo.allLearningCardIds()
      )
    } else {
      Future.None
    }
  }

  def checkCompletedCourse(user: SignedInUser,
                           courseId: String,
                           cardIds: Seq[String]): Future[Option[String]] = Future {

    val isCompleted = repository.getNotLearnCardIds(user.username, courseId, cardIds).isEmpty

    if (isCompleted) {
      repository.addCompletedCourse(user.username, courseId)
      Some(courseId)
    } else {
      None
    }

  }

  override def completionCheck(user: SignedInUser,
                               learningInfo: UserLearningInfo,
                               courseId: String,
                               sessionId: String,
                               topicId: String): Future[CourseProgress] = {

    for {
      cardIds <- deckService.getDeck(topicId).map(_.cards.getOrElse(ListBuffer.empty))
      topicCompleted = repository.getTopicNotLearnCardIds(user.username,
        topicId,
        cardIds).isEmpty
      courseCompleted = repository.getNotLearnCardIds(user.username,
        courseId,
        learningInfo.allLearningCardIds()).isEmpty

    } yield {
      if (courseCompleted)
        repository.addCompletedCourse(user.username, courseId)
      if (topicCompleted)
        repository.addCompletedTopic(user.username, courseId)
      CourseProgress(
        completedCourseId = if (courseCompleted) Some(courseId) else None,
        completedTopicId = if (topicCompleted) Some(topicId) else None
      )
    }

  }


  override def resetLearningInfo(user: SignedInUser): Future[Option[UserLearningInfo]] = Future {
    val learningInfo = UserLearningInfo(
      username = user.username,
      courseId = None,
      cardIds = None
    )
    val r = repository.addLearningInfo(learningInfo)
    if(r)  Option(learningInfo) else None
  }

  override def addLearningInfo(learningInfo: UserLearningInfo): Future[Boolean] = Future {
    repository.addLearningInfo(learningInfo)
  }


  override def getLearningInfo(user: SignedInUser): Future[Option[UserLearningInfo]] = Future {
    repository.getLearningInfo(user.username)
  }

  override def getOrCreateLearningInfo(user: SignedInUser): Future[UserLearningInfo] = {
    repository.getLearningInfo(user.username) match {
      case Some(x) => Future.value(x)
      case _ => initDefault(user).map({
        case Some(x) => x
        case _ => throw InternalError(Some("Can't init learn."))
      })
    }
  }

  def updateLearningCards(user: SignedInUser, learningInfo: UserLearningInfo): Future[UserLearningInfo] = {
    for {
      uncompletedCards <- loadCourseAndCards(user.username, learningInfo.courseId.getOrElse(""))
      newLearningInfo = learningInfo.copy(
        cardIds = Some(uncompletedCards)
      )
      _ = newLearningInfo.updateSuggestionRange()
      _ = repository.addLearningInfo(newLearningInfo)
    } yield {
      newLearningInfo
    }
  }


  override def changeCourse(user: SignedInUser, courseId: String): Future[Option[UserLearningInfo]] = {
    for {
      uncompletedCards <- loadCourseAndCards(user.username, courseId)
      learningInfo = UserLearningInfo(
        username = user.username,
        courseId = Some(courseId),
        cardIds = Some(uncompletedCards)
      )
      _ = learningInfo.initSuggestionRange()

      status = repository.addLearningInfo(learningInfo)
    } yield status match {
      case true => Option(learningInfo)
      case _ => None
    }
  }

  private def initDefault(user: SignedInUser): Future[Option[UserLearningInfo]] = {

    for {
      learningInfo <- Future.value(UserLearningInfo(
        username = user.username,
        courseId = None,
        cardIds = None))
      _ = learningInfo.initSuggestionRange()
      status = repository.addLearningInfo(learningInfo)
    } yield status match {
      case true => Option(learningInfo)
      case _ => None
    }
  }


  private def loadCardId(username: String, courseId: String) = {

    def getCardIds(deckIds: Seq[String]): Future[Seq[String]] = {

      for {
        deckMap <- deckService.getDeckAsMap(deckIds)
        notFoundDeckIds = deckIds.filterNot(deckMap.contains(_))
      } yield {
        if (notFoundDeckIds.nonEmpty)
          throw InternalError(Some(s"There are some missing decks:${notFoundDeckIds}"))

        deckIds.map(deckMap.get(_))
          .filter(_.isDefined)
          .map(_.get)
          .map(_.cards.getOrElse(ListBuffer.empty))
          .flatten
      }

    }

    for {
      course <- courseService.get(courseId)
      deckIds = course.flatMap(_.deckIds).getOrElse(ListBuffer.empty)
      cardIds <- getCardIds(deckIds)
    } yield cardIds
  }


  private def loadCourseAndCards(username: String, courseId: String) = Profiler(s"$clazz.loadCourseAndCards") {
    for {
      course <- courseService.get(courseId)
      journeyIds = course.flatMap(_.journeyIds).getOrElse(ListBuffer.empty)
      journeyInfos <- journeyService.multiGet(journeyIds)
      deckJourneyMap = journeyInfos.flatMap(journey =>{
        val deckIds = journey.deckIds.getOrElse(ListBuffer.empty)

        deckIds.map(deckId => deckId -> journey.id)
      }).toMap

      deckIds = course.flatMap(_.deckIds).getOrElse(ListBuffer.empty)
      deckSessionMap = deckIds.map(deckId => deckId -> deckJourneyMap.get(deckId).getOrElse("")).toMap

      cards <- loadCourseUncompletedCard(username, courseId, deckIds)

      r = cards.map(card => LearnCard(
        courseId,
        deckSessionMap.get(card.deckId.getOrElse("")).getOrElse(""),
        card.deckId.getOrElse(""),
        cardId = card.id
      ))
    } yield {
      info(
        s"""
           |Username: $username
           |Load ${deckIds.size} decks and ${r.size} cards from course: $courseId
           |""".stripMargin)
      r
    }
  }


  private def loadCourseUncompletedCard(username: String,
                                       courseId: String,
                                       deckIds: Seq[String]): Future[Seq[Card]] = Profiler(s"$clazz.loadCourseUncompletedCard") {

    def loadUnCompletedCards(deckIds: Seq[String],
                             deckMap: Map[String, Deck]) = Profiler(s"$clazz.loadCourse.loadUncompletedCards") {
      val cardIds = deckIds.map(deckMap.get)
        .filter(_.isDefined)
        .map(_.get)
        .flatMap(_.cards.getOrElse(ListBuffer.empty))
      val unCompletedCardIds = repository.getNotLearnCardIds(
        username,
        courseId,
        cardIds
      )

      cardService.getCardAsMap(unCompletedCardIds)
        .map(_.filter(_._2.hasFront))
        .map(cardData => (unCompletedCardIds, cardData))
    }

    for {
      deckMap <- deckService.getDeckAsMap(deckIds)
      (unCompletedCardIds, cardMap) <- loadUnCompletedCards(deckIds, deckMap)

    } yield {
      unCompletedCardIds.map(cardMap.get(_))
        .filter(_.isDefined)
        .map(_.get)
    }
  }

  override def isNewCourse(user: SignedInUser, courseId: String): Future[Boolean] = Future {
    !repository.isLearningCourse(user.username, courseId)
  }

  override def isNewSection(recipient: SignedInUser, sectionId: String): Future[Boolean] = Future {
    !repository.isLearningSection(recipient.username, sectionId)
  }

  override def isNewTopic(recipient: SignedInUser, topicId: String): Future[Boolean] = Future {
    !repository.isLearningTopic(recipient.username, topicId)
  }

  override def markLearning(user: SignedInUser,
                            courseId: Option[String],
                            sectionId: Option[String],
                            topicId: Option[String]): Future[Boolean] = Future {
    repository.markLearning(user.username,
      courseId,
      sectionId,
      topicId)
  }
}

