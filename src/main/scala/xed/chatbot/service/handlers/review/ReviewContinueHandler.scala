package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor

/**
 * @author andy
 * @since 2/17/20
 **/
case class ReviewContinueHandler(nlpService: NLPService,
                                 botConfig: BotConfig,
                                 analytics: Analytics,
                                 processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {
    processor.sendQuestionMessage(context)
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }
}
