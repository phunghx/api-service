package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._


case class ErrorHandler(nlpService: NLPService,
                        botConfig: BotConfig,
                        analytics: Analytics) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {


    val actionInfo = context.actionInfo

    val card = XCard(
      version = 1,
      body = Seq(
        Text(botConfig.getErrorMsg())
      ),
      actions = Seq.empty,
      fallbackText = None,
      background = None,
      speak = None,
      suggestions = Seq.empty
    )


    val message = ChatMessage.create(
      card = Some(card),
      currentAction = if (actionInfo != null && actionInfo.isRequiredParamCompleted && actionInfo.contexts.nonEmpty) Some(actionInfo) else None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Some(context.recipient.username)
    )

    context.write(message)

    Future.Unit
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

}