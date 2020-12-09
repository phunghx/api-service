package xed.chatbot.service.handlers.test

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.design.{Component, Container}
import xed.api_service.domain.request.ReviewRequest
import xed.api_service.domain.{Card, SRSCard, SRSSource}
import xed.api_service.service.{ CardService, SRSService}
import xed.chatbot.domain._
import xed.profiler.Profiler

/**
 * @author andy
 * @since 2/17/20
 **/

case class TestReviewProcessor(botConfig: BotConfig,
                               srsService: SRSService,
                               cardService: CardService) extends TestProcessor {


  val actionType: String = IntentActionType.REVIEW

  override def startWith(context: BotContext): Future[Unit] = {
    for {
      cardIds <- srsService.getDueCardIds(context.recipient, SRSSource.BOT)
      _ <- if(cardIds.nonEmpty)
        initAndStart(context, cardIds)
      else {
        context.removeContextParam(BotContext.REVIEW_CTX)
        sendNoCardExplainMessage(context)
        sendHelpMessage(context)
        Future.Unit
      }
    } yield {

    }
  }


  override def setupContextAndStart(context: BotContext, cardIds: Seq[String]): Future[Unit] =  {
    for {
      _ <- initAndStart(context, cardIds)
    } yield {

    }
  }


  override def submitAnswerAndUpdateScore(context: BotContext,
                                          cardId: String,
                                          isCorrect: Boolean,
                                          isSkip: Boolean = false): Future[Boolean] = Profiler(s"$clazz.submitAnswerAndUpdateScore") {

    val examinationData = context.getExaminationData()

    val reviewRequest = ReviewRequest(cardId,
      isSkip match {
        case true => ReviewRequest.DONT_KNOWN_ANSWER
        case _ => if (isCorrect)
            ReviewRequest.CORRECT_ANSWER
          else
            ReviewRequest.INCORRECT_ANSWER

      },
      duration = Some(examinationData.getDuration())
    )

    srsService.review(context.recipient, SRSSource.BOT, reviewRequest).map(reviewResultInfo => {
      examinationData.addFinalResult(cardId,
        isCorrect,
        isSkip,
        reviewRequest.duration.getOrElse(0))

      context.updateContextData(BotContext.EXAMINATION_DATA, examinationData)
      true
    })

  }

  def onQuestionCompleted(context: BotContext, cardId: String): Future[Unit] = Profiler(s"$clazz.onQuestionCompleted") {
    for {
      _ <- sendDictionaryBackCard(context, cardId)
      hasNextCard <- gotoNextQuestion(context)
      _ <- if (hasNextCard) {
        sendNextDueDateMessage(context, cardId)
      } else {
        submitScoreAndCompleteChallenge(context)
      }
    } yield {

    }

  }

  override def submitScoreAndCompleteChallenge(context: BotContext): Future[Unit] = Future {
    val examinationData = context.getExaminationData()

    context.removeContextParam(BotContext.REVIEW_CTX)
    context.write(buildResultMessage(context, examinationData))
    context.write(buildSuggestToLearnMessage(context))
  }


  protected def gotoNextQuestion(context: BotContext): Future[Boolean] = Profiler(s"$clazz.gotoNextQuestion") {

    val reviewData = context.getExaminationData()
    for {
      reviewCard <- reviewData.getNextCardId().fold[Future[Option[SRSCard]]](Future.None)(id => {
        srsService.getReviewCards(context.recipient,SRSSource.BOT, Seq(id)).map(_.headOption)
      })

      (front, frontIndex) = reviewCard match {
        case Some(card) => card.getFrontRandomly()
        case _ => (None, 0)
      }

      r = if (front.isDefined) {
        reviewData.setCurrentCard(reviewData.currentCardIndex + 1,
          frontIndex,
          reviewCard,
          front
        )
        context.updateContextData(BotContext.EXAMINATION_DATA, reviewData)
        true
      } else {
        false
      }
    } yield {
      r
    }
  }

  override def sendNoCardExplainMessage(context: BotContext): Unit = {

    val msg = ChatMessage.create(
      card = Some( XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getNoDueCardMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

  }

  override def sendTotalCardExplainMessage(context: BotContext, totalCard: Int) : Unit = {
    val msg =  ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getHasDueCardMsg(totalCard))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }

  override def buildQuestionMessage(context: BotContext): Future[ChatMessage] = {

    def buildBody(frontSide: Option[Container]): Seq[Component] = {
      val isAction = frontSide.map(_.hasAction()).getOrElse(false)
      if (isAction) {
        Seq(frontSide)
          .filter(_.isDefined)
          .map(_.get)
      } else {
        Seq(Some(Text(botConfig.getReviewInfoCardMsg())), frontSide)
          .filter(_.isDefined)
          .map(_.get)
      }
    }

    val reviewParam = context.getExaminationData()
    val index = reviewParam.currentCardIndex
    val frontIndex = reviewParam.currentFrontIndex
    val cardIds = reviewParam.cardIds
    val cardId = cardIds(index)

    val user = context.recipient
    for {
      (frontSide: Option[Container], _) <- srsService.getReviewCards(user,SRSSource.BOT, Seq(cardId))
        .map(_.head)
        .map(_.getFrontAt(Some(frontIndex)))

      isActionComponent = frontSide.map(_.hasAction()).getOrElse(false)

      message = ChatMessage.create(
        card = Some(XCard(
          version = 1,
          body = buildBody(frontSide),
          actions = if (isActionComponent) Seq.empty[UserAction] else {
            Seq(
              PostBackUAction(botConfig.getDontKnownAction().title, botConfig.getDontKnownAction().value),
              PostBackUAction(botConfig.getKnownAction().title, botConfig.getKnownAction().value)
            )
          },
          fallbackText = None,
          background = None,
          speak = None,
          suggestions = Seq(
            PostBackUAction(
              botConfig.getSkipAction().title,
              botConfig.getSkipAction().value
            ),
            PostBackUAction(
              botConfig.getExitAction().title,
              botConfig.getExitAction().value
            )
          )
        )),
        currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
        sender = Some(ChatMessage.KIKIBOT),
        recipient = Option(user.username)
      )
    } yield {
      context.updateContextData(BotContext.EXAMINATION_DATA, reviewParam)
      message
    }
  }


  override def sendDictionaryBackCard(context: BotContext,
                                      card: Card,
                                      enableLearnSuggestion: Boolean): Future[Boolean] = Future {

    val backCard = card.backCard()

    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          backCard
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

    true
  }





}
