package xed.chatbot.service.sys_handlers

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain.{BotConfig, BotContext, ChatMessage, IntentActionInfo}
import xed.chatbot.service.handlers.ActionHandler

/**
 * @author andy
 * @since 4/17/20
 **/
case class SysExitHandler(nlpService: NLPService,
                          botConfig: BotConfig,
                          analytics: Analytics,
                          handlers:  Map[String,ActionHandler]) extends ActionHandler {


  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    chatMessage.currentAction match {
      case Some(currentAction) => dispatchExit(context,currentAction, chatMessage)
      case _ => handleExit(context, chatMessage)
    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }


  def dispatchExit(context: BotContext,
                   currentAction: IntentActionInfo,
                   chatMessage: ChatMessage): Future[Unit] = {
    handlers.get(currentAction.actionType) match {
      case Some(handler) => handler.handleExit(context, chatMessage )
      case _ => handleExit(context, chatMessage)
    }

  }



}
