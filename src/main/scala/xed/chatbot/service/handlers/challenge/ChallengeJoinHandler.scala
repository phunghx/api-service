package xed.chatbot.service.handlers.challenge

import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.exception.InternalError
import xed.api_service.service.{Analytics, NLPService, Operation}
import xed.chatbot.domain._
import xed.chatbot.domain.challenge.{Challenge, ChallengeType}
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.{ChallengeService, ChallengeTemplateService, LeaderBoardService}
import xed.profiler.Profiler



case class ChallengeJoinHandler(nlpService: NLPService,
                                botConfig: BotConfig,
                                analytics: Analytics,
                                templateService: ChallengeTemplateService,
                                challengeService: ChallengeService,
                                leaderBoardService: LeaderBoardService) extends ActionHandler {

  final val challengeSelector = MyChallengeSelector(
    nlpService,
    botConfig,
    challengeService
  )

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    val joinChallengeData = context.getChallengeJoinContextData()

    val fn = for {
      _ <- joinChallengeData.challengeId match {
        case Some(challengeId) => challengeService.getChallengeInfo(challengeId.toInt)
          .flatMap(challenge => {
            challengeService.getQuestionCount(challenge.questionListId).map(count => (challenge, count))
          }).map(entry => {
          context.write(buildChallengeIntroMessage(context, entry._1, entry._2))
        })
        case _ => showChallengeSelectorAndWaitFor(context, true, chatMessage)
      }
      _ <- leaderBoardService.setUserShortProfile(context.recipient.username)
      joinOK <- if(joinChallengeData.challengeId.isDefined) {
        leaderBoardService.initialPointIfNotFound(
          joinChallengeData.challengeId.get,
          context.recipient.username).map({
          case true =>
            analytics.log(Operation.BOT_JOIN_CHALLENGE, context.recipient.userProfile, Map(
            "username" -> context.recipient.username,
            "challenge_id" -> joinChallengeData.challengeId
          ))

          case _ => throw InternalError(Some("Can't join this challenge at this time."))
        })
      } else Future.False
    } yield {

    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        context.removeChallengeContextData()
        context.write(buildInvalidChallengeMessage(context, e))
        context.write(ActionHandler.buildHelpMessage(context, botConfig))
        Future.Unit
    })
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    val joinChallengeData = context.getChallengeJoinContextData()
    val fn = joinChallengeData.challengeId match {
      case Some(challengeId) => challengeService.getChallengeInfo(challengeId.toInt)
        .flatMap(challenge => {
          challengeService.getQuestionCount(challenge.questionListId).map(count => (challenge, count))
        }).map(entry => {
        context.write(buildChallengeIntroMessage(context, entry._1, entry._2))
      })
      case _ => showChallengeSelectorAndWaitFor(context, false, chatMessage)

    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        context.removeChallengeContextData()
        context.write(buildInvalidChallengeMessage(context, e))
        Future.Unit
    })


  }


  private def showChallengeSelectorAndWaitFor(context: BotContext,
                                              isFirstTime: Boolean,
                                              chatMessage: ChatMessage): Future[Unit] = Profiler(s"$clazz.challengeSelectorAndWaitFor") {

    if (isFirstTime)
      challengeSelector.setupAndShowListing(context).map({
        case true => true
        case _ => throw InternalError(Some("No challenge was found"))
      }).map(_ => {})
    else
      challengeSelector.processUserReply(context, chatMessage).map(_ => {})
  }


  private def buildChallengeIntroMessage(context: BotContext,
                                    challenge: Challenge,
                                    questionCount: Int): ChatMessage = {

    val introMsg = challenge.challengeType.getOrElse(ChallengeType.DEFAULT) match {
      case ChallengeType.STREAKING => botConfig.getStreakingChallengeIntroMessage(challenge, questionCount)
      case _ => botConfig.getChallengeIntroMessage(challenge, questionCount)
    }

    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(Text(introMsg)),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions =  Seq(
          PostBackUAction("Play", s"Play challenge ${challenge.challengeId}")
        )
      )),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    message
  }


  private def buildInvalidChallengeMessage(context: BotContext, ex: Throwable): ChatMessage = {

    ChatMessage.create(
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
      currentAction = Option(context.actionInfo).map(_.copy(
        actionType = IntentActionType.UNKNOWN
      )),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }


}

