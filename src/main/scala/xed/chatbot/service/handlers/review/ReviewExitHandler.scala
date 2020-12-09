package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService, Operation}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor


case class ReviewExitHandler(nlpService: NLPService,
                             botConfig: BotConfig,
                             analytics: Analytics,
                             processor: TestProcessor
                            ) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    val examinationData = context.getExaminationData()
    context.write(processor.buildResultMessage(context, examinationData))
    processor.sendSuggestToLearnMessage(context)
    Future.Unit
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }


}
