package xed.chatbot.service.handlers.test

import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.design.{Component, Container}
import xed.api_service.domain.exception.InternalError
import xed.api_service.domain.{Card, SRSCard, SRSSource}
import xed.api_service.service.{CardService, SRSService}
import xed.api_service.util.Utils
import xed.chatbot.domain.BotContext.EXAMINATION_DATA
import xed.chatbot.domain._
import xed.chatbot.domain.challenge.ChallengeType
import xed.chatbot.domain.leaderboard.LeaderBoardItem
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.{ChallengeService, LeaderBoardService}
import xed.leaderboard.domain.thrift.TLeaderBoardType
import xed.profiler.Profiler

/**
 * @author andy
 * @since 2/17/20
 **/
case class TestChallengeProcessor(botConfig: BotConfig,
                                  challengeService: ChallengeService,
                                  leaderBoardService: LeaderBoardService,
                                  srsService: SRSService,
                                  cardService: CardService) extends TestProcessor {


  val actionType: String = IntentActionType.CHALLENGE_PLAY


  override def setupContextAndStart(context: BotContext, cardIds: Seq[String]): Future[Unit] = ???


  override def startWith(context: BotContext): Future[Unit] = {
    val challengeData = context.getChallengeContextData()

    val fn = for {
      updateProfileOK <- leaderBoardService.setUserShortProfile(context.recipient.username)
      _ <- if(challengeData.challengeId.isDefined) {
        leaderBoardService.initialPointIfNotFound(
          challengeData.challengeId.get,
          context.recipient.username)
      } else Future.exception(InternalError(Some("Challenge not found.")))

      challenge <- challengeService.getChallengeInfo(challengeData.challengeId.map(_.toInt).get)
      _ = context.updateContextData(BotContext.CHALLENGE_CTX, challengeData.copy(
        challenge = Option(challenge)
      ))
      joinOK <- challengeService.joinChallenge(context.recipient.username, challenge.challengeId)
      questionIds <- challengeService.getQuestionIds(challenge.questionListId)
      _ <- initAndStart(context, Utils.getRandomSubset(questionIds))
    } yield {

    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        context.removeChallengeContextData()
        context.write(buildInvalidChallengeMessage(context, e))
        context.write(ActionHandler.buildHelpMessage(context,botConfig))
        Future.Unit
    })

  }

  override def onQuestionCompleted(context: BotContext, cardId: String): Future[Unit] = {

    val challengeData = context.getChallengeContextData()
    context.updateContextData(BotContext.CHALLENGE_CTX, challengeData)
    context.updateContextData(BotContext.CHALLENGE_FOLLOWCTX, EmptyContextData())
    for {
      hasNextQuestion <- gotoNextQuestion(context)
      isChallengeCompleted = checkChallengeResult(context)
      _ <- if (!hasNextQuestion || isChallengeCompleted) {
        submitScoreAndCompleteChallenge(context)
      } else {
        sendQuestionMessage(context)
      }
    } yield {

    }
  }


  /**
   * Check the result whether this user can continue to play the challenge or not
   * @param context
   * @return
   */
  private def checkChallengeResult(context: BotContext): Boolean = {
    val challengeData = context.getChallengeContextData()
    val examinationData  = context.getExaminationData()

    val challenge = challengeData.challenge.get

    val isEnd = challenge.challengeType.getOrElse(ChallengeType.DEFAULT) match {
      case ChallengeType.STREAKING =>
        examinationData.getIncorrectQuestionCount() > 0 || examinationData.getSkipQuestionCount() > 0
      case _ =>
        examinationData.cardIds == null || examinationData.cardIds.isEmpty
    }
    if(isEnd) {
      info(
        s"""
           | checkChallengeResult
           | ${context.recipient.username}
           | $challenge
           | $examinationData
           |""".stripMargin)
    }
    isEnd
  }

  override def submitScoreAndCompleteChallenge(context: BotContext): Future[Unit] =  {
    context.removeContextParam(BotContext.CHALLENGE_FOLLOWCTX)
    val challengeData = context.getChallengeContextData()
    val examinationData = context.getExaminationData()
    for {
      _ <- leaderBoardService.setPointIfHighest(
        challengeData.challengeId.get,
        context.recipient.username,
        examinationData.beginTime,
        examinationData.getDuration(),
        examinationData.totalCount,
        examinationData.getTotalAnsweredQuestions(),
        examinationData.getPoints()
      )

      myRank <- leaderBoardService.getRank(
        challengeData.challengeId.get,
        context.recipient.username,
        Some(TLeaderBoardType.AllTime))
    } yield {
      val msg = buildChallengeResultMessage(context,myRank, examinationData)
      context.write(msg)
    }

  }

  override def submitAnswerAndUpdateScore(context: BotContext,
                                          cardId: String,
                                          isCorrect: Boolean,
                                          isSkip: Boolean = false): Future[Boolean] = Profiler(s"$clazz.submitAnswerAndUpdateScore") {
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

  override def sendTotalCardExplainMessage(context: BotContext, totalCard: Int): Unit = {

  }


  protected override def gotoNextQuestion(context: BotContext): Future[Boolean] = Profiler(s"$clazz.gotoNextQuestion") {

    val reviewData = context.getExaminationData()
    for {
      cardWithReviewInfo <- reviewData.getNextCardId().fold[Future[Option[SRSCard]]](Future.None)(id => {
        cardService.getReviewCardAsList(context.recipient, SRSSource.BOT, Seq(id)).map(_.headOption)
      })
      (front, frontIndex) = cardWithReviewInfo.map(_.getFrontAt(None)).getOrElse((None, 0))
      r = if (front.isDefined) {
        reviewData.setCurrentCard(reviewData.currentCardIndex + 1,
          frontIndex,
          cardWithReviewInfo,
          front
        )
        context.updateContextData(EXAMINATION_DATA, reviewData)
        true
      } else  false
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
              botConfig.getCompleteChallengeAction().title,
              botConfig.getCompleteChallengeAction().value
            )
          )
        )),
        currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
        sender = Some(ChatMessage.KIKIBOT),
        recipient = Option(user.username)
      )
    } yield {
      context.updateContextData(EXAMINATION_DATA, reviewParam)
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
          PostBackUAction("Next Question", "Next")
        )
        else Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

    true
  }


  override def buildResultMessage(context: BotContext, examinationData: ExaminationData): ChatMessage = ???


  def buildChallengeResultMessage(context: BotContext,
                                  myRank: LeaderBoardItem,
                                  examinationData: ExaminationData): ChatMessage = {

    val challengeData = context.getChallengeContextData()
    val challenge = challengeData.challenge.get

    val correctCount = examinationData.getCorrectQuestionCount()
    val totalCount = examinationData.totalCount

    val congratulationMsg = if(myRank.rank == 1) {
      botConfig.getChallengeChampionResultMsg(
        challenge.name.getOrElse(""),
        correctCount,
        totalCount,
        myRank
      )
    } else {
      botConfig.getChallengeResultMsg(
        challenge.name.getOrElse(""),
        correctCount,
        totalCount,
        myRank
      )
    }

    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(Text(congratulationMsg)),
        actions = Seq(
          ShareChallengeUAction(
            "Challenge your friends",
            challenge.challengeId.toString),
          PostBackUAction(
            "Play again",
            s"Play ${ challenge.challengeId.toString}")
        ),
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    message
  }


  def buildInvalidChallengeMessage(context: BotContext, ex: Throwable): ChatMessage = {

    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(ex.getMessage)
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

    message
  }

}
