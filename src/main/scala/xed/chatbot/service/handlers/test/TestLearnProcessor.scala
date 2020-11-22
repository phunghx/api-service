package xed.chatbot.service.handlers.test

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.design.{Component, Container}
import xed.api_service.domain.{Card, SRSCard, SRSSource}
import xed.api_service.service.{CardService, SRSService}
import xed.chatbot.domain._
import xed.profiler.Profiler

import scala.collection.mutable.ListBuffer

/**
 * @author andy
 * @since 2/17/20
 **/
case class TestLearnProcessor(botConfig: BotConfig,
                              srsService: SRSService,
                              cardService: CardService) extends TestProcessor {


  val actionType: String = IntentActionType.LEARN_TEST


  override def startWith(context: BotContext): Future[Unit] = {
    val data = context.getExaminationData()
    initAndStart(context, data.cardIds)
  }


  override def setupContextAndStart(context: BotContext, cardIds: Seq[String]): Future[Unit] = {
    context.removeLearnContext()
    context.updateContextData(BotContext.LEARN_TEST_CTX, EmptyContextData())
    context.updateContextData(BotContext.LEARN_TEST_FOLLOWCTX, EmptyContextData())

    context.updateContextData(BotContext.EXAMINATION_DATA,ExaminationData(
      cardIds = cardIds,
      totalCount = cardIds.size,
      currentCardIndex = -1,
      currentFrontIndex = 0,
      ListBuffer.empty[String]
    ))
    initAndStart(context, cardIds)
  }


  override def onQuestionCompleted(context: BotContext, cardId: String): Future[Unit] = {

    val param = context.getLearnContextData()
    param.setCardToLearn(cardId)
    context.updateContextData(BotContext.LEARN_VOCABULARY_CTX, param)
    context.updateContextData(BotContext.LEARN_VOCABULARY_FOLLOWCTX, EmptyContextData())
    context.removeContextParam(BotContext.LEARN_TEST_CTX)
    context.removeContextParam(BotContext.LEARN_TEST_FOLLOWCTX)
    for {
      _ <- sendDictionaryBackCard(context, cardId,true)
    } yield {

    }
  }

  override def submitScoreAndCompleteChallenge(context: BotContext): Future[Unit] = Future{}


  override def submitAnswerAndUpdateScore(context: BotContext,
                                          cardId: String,
                                          isCorrect: Boolean,
                                          isSkip: Boolean = false): Future[Boolean] =  Profiler(s"$clazz.submitAnswerAndUpdateScore")  {

    Future {
      val examinationData = context.getExaminationData()
      examinationData.addFinalResult(cardId,
        isCorrect,
        isSkip,
        examinationData.getDuration())

      examinationData.resetForNewQuestion()
      context.updateContextData(BotContext.EXAMINATION_DATA, examinationData)
      true
    }
  }


  override def sendNoCardExplainMessage(context: BotContext): Unit = {}

  override  def sendTotalCardExplainMessage(context: BotContext, totalCard: Int) : Unit = {

  }


  protected override def gotoNextQuestion(context: BotContext): Future[Boolean] = Profiler(s"$clazz.gotoNextQuestion") {

    val reviewData = context.getExaminationData()
    for {
      cardWithReviewInfo <- reviewData.getNextCardId().fold[Future[Option[SRSCard]]](Future.None)(id => {
        cardService.getReviewCardAsList(context.recipient, SRSSource.BOT, Seq(id)).map(_.headOption)
      })
      (front, frontIndex) = cardWithReviewInfo.map(_.getFrontAt(Some(0))).getOrElse((None, 0))
      r = if (front.isDefined) {
        reviewData.setCurrentCard(reviewData.currentCardIndex + 1,
          frontIndex,
          cardWithReviewInfo,
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

      (frontSide: Option[Container], _) <- cardService.getReviewCardAsList(user, SRSSource.BOT, Seq(cardId))
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
        suggestions = if (enableLearnSuggestion) Seq(
          PostBackUAction(botConfig.getLearnYesAction().title, botConfig.getLearnYesAction().value),
          PostBackUAction(botConfig.getLearnNoAction().title, botConfig.getLearnNoAction().value),
          PostBackUAction("Exit", "Exit")
        )
        else Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.LEARN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

    true
  }




}
