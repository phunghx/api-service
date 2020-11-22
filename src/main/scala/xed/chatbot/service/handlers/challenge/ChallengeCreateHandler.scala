package xed.chatbot.service.handlers.challenge

import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.exception.InternalError
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.{ChallengeService, ChallengeTemplateService, LeaderBoardService}
import xed.profiler.Profiler


case class ChallengeCreateHandler(nlpService: NLPService,
                                  botConfig: BotConfig,
                                  analytics: Analytics,
                                  templateService: ChallengeTemplateService,
                                  challengeService: ChallengeService,
                                  leaderBoardService: LeaderBoardService) extends ActionHandler {

  final val templateSelector = ChallengeTemplateSelector(
    nlpService,
    botConfig,
    templateService,
    challengeService
  )

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    context.removeChallengeContextData()
    context.removeLearnIntroductionContext()
    context.removeLearnTestContext()

    val fn = for {
      _ <- showChallengeSelectorAndWaitFor(context, true, chatMessage)
      updateProfileOK <- leaderBoardService.setUserShortProfile(context.recipient.username)
    } yield {

    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        context.write(buildInvalidChallengeMessage(context, e))
        context.write(ActionHandler.buildHelpMessage(context,botConfig))
        Future.Unit
    })
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    showChallengeSelectorAndWaitFor(
      context,
      false,
      chatMessage).transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        context.write(buildInvalidChallengeMessage(context, e))
        Future.Unit
    })


  }


  private def showChallengeSelectorAndWaitFor(context: BotContext,
                                              isFirstTime: Boolean,
                                              chatMessage: ChatMessage): Future[Unit] = Profiler(s"$clazz.challengeSelectorAndWaitFor") {
    def ensureListingExists(x: Boolean): Boolean = {
      x match {
        case true => true
        case _ => throw InternalError(Some("No challenge was found"))
      }
    }
    for {
      r <- if (isFirstTime)
        templateSelector.setupAndShowListing(context).map(ensureListingExists(_))
      else
        templateSelector.processUserReply(context, chatMessage)
    } yield {

    }
  }

  private def buildInvalidChallengeMessage(context: BotContext, ex: Throwable): ChatMessage = {

    context.removeChallengeContextData()

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
      currentAction = Option(context.actionInfo).map(_.copy(
        actionType = IntentActionType.UNKNOWN
      )),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    message
  }


}

