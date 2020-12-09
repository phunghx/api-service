package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._


case class ForwardHandler(nlpService: NLPService,
                          botConfig: BotConfig,
                          analytics: Analytics) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage) = {

    val message = buildForwardResponse(context, chatMessage)
    context.write(message)
    Future.Unit
  }


  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

  private def buildForwardResponse(context: BotContext, chatMessage: ChatMessage)  = {

    val actionInfo = context.actionInfo

    val card = XCard(
      version = 1,
      body = Seq(
        Text(actionInfo.fulfillmentText.getOrElse(""))
      ),
      actions = parseActionsFromPayload(actionInfo),
      fallbackText = None,
      background = None,
      speak = None,
      suggestions = parseSuggestionsFromPayload(actionInfo)
    )


    ChatMessage.create(
      card = Some(card),
      currentAction = if (actionInfo.isRequiredParamCompleted && actionInfo.contexts.nonEmpty)
        Some(actionInfo)
      else None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Some(context.recipient.username)
    )


  }




}