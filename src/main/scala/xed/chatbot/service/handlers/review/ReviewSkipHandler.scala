package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor

case class ReviewSkipHandler(nlpService: NLPService,
                             botConfig: BotConfig,
                             analytics: Analytics,
                             processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    for {
      cardId <- processor.getCurrentCardIdToReview(context)
      isCompleted <- processor.submitAnswerAndUpdateScore(
        context,
        cardId,
        false,
        true)
      _ <- if (!isCompleted) Future.Unit else {
        processor.onQuestionCompleted(context, cardId)
      }
    } yield {
      context.updateContextData(BotContext.REVIEW_CTX,EmptyContextData())
    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

}

