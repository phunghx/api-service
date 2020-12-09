package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._

case class HelpHandler(nlpService: NLPService,
                       botConfig: BotConfig,
                       analytics: Analytics) extends ActionHandler {
  private val processor = DefaultProcessor("", botConfig)

  override def handleCall(context: BotContext, chatMessage: ChatMessage) = {

    processor.sendHelpMessage(context)
    Future.Unit

  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }
}