package xed.chatbot.service

import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw}
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.ActionResolveEngine
import xed.chatbot.domain.{BotConfig, ChatMessage, IntentActionInfo, IntentActionType}
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.sys_handlers.SysExitHandler
import xed.profiler.Profiler
import xed.userprofile.SignedInUser


trait BotService extends Logging {
  protected lazy val clazz = getClass.getSimpleName

  def processMessage(sender: SignedInUser, chatMessage: ChatMessage): Future[Seq[ChatMessage]]

  def conversationStart(sender: SignedInUser,  message: ChatMessage): Future[Seq[ChatMessage]]

}



case class KiKiBotServiceImpl @Inject()(resolveActionEngine: ActionResolveEngine,
                                        nlpService:  NLPService,
                                        botConfig: BotConfig,
                                        analytics: Analytics,
                                        handlers:  Map[String,ActionHandler],
                                        conversationStartHandler: ActionHandler,
                                        forwardHandler: ActionHandler,
                                        fallbackHandler: ActionHandler,
                                        errorHandler: ActionHandler) extends BotService {

  val sysHandlers = Map(
    IntentActionType.SYS_EXIT -> SysExitHandler(nlpService,botConfig, analytics, handlers)
  )

  override def processMessage(sender: SignedInUser, chatMessage: ChatMessage): Future[Seq[ChatMessage]] = {
    val fn = for {
      actionInfo <- resolveActionEngine.resolve(chatMessage)
      r <- if(sysHandlers.contains(actionInfo.actionType)) {
        handleSysAction(sender, actionInfo, chatMessage)
      } else if (actionInfo.isFallback)
        handleFallbackAction(sender, actionInfo, chatMessage)
      else {
        handleAction(sender, actionInfo, chatMessage)
      }
    } yield {
      r
    }

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        logger.error(clazz, e)
        errorHandler.handle(sender,
          chatMessage.currentAction.getOrElse(ActionResolveEngine.buildFallbackActionInfo(chatMessage)),
          true,
          chatMessage)
    })

  }

  private def handleSysAction(sender: SignedInUser,
                           actionInfo: IntentActionInfo,
                           chatMessage: ChatMessage): Future[Seq[ChatMessage]] = Profiler(s"$clazz.handleSysAction") {

    val handler = sysHandlers.get(actionInfo.actionType)
    handler.fold({
      forwardHandler.handle(sender,
        actionInfo,
        true,
        chatMessage)
    })(_.handle(sender, actionInfo, true, chatMessage))
  }


  private def handleAction(sender: SignedInUser,
                           actionInfo: IntentActionInfo,
                           chatMessage: ChatMessage): Future[Seq[ChatMessage]] = Profiler(s"$clazz.handleAction") {

    val handler = if (actionInfo.isRequiredParamCompleted)
      handlers.get(actionInfo.actionType)
    else None

    handler.fold({
      forwardHandler.handle(sender,
        actionInfo,
        true,
        chatMessage)
    })(_.handle(sender, actionInfo, true, chatMessage))
  }


  private def handleFallbackAction(sender: SignedInUser,
                                   actionInfo: IntentActionInfo,
                                   chatMessage: ChatMessage): Future[Seq[ChatMessage]] = Profiler(s"$clazz.handleFallbackAction") {

    chatMessage.currentAction match {
      case None =>
        fallbackHandler.handle(sender,
          actionInfo,
          true,
          chatMessage)
      case Some(currentAction) =>
        handlers.get(currentAction.actionType)
          .fold({
            fallbackHandler.handle(sender,
              actionInfo,
              true,
              chatMessage
            )
          })(_.handle(sender, currentAction, false, chatMessage))
    }
  }

  override def conversationStart(sender: SignedInUser,  message: ChatMessage): Future[Seq[ChatMessage]] = {
    conversationStartHandler.handle(
      sender,
      ActionResolveEngine.buildFallbackActionInfo(message),
      true,
      message)
  }

}



