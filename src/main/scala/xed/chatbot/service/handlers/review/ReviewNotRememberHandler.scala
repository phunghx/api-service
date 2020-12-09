package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain.{BotConfig, BotContext, ChatMessage, EmptyContextData}
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor

/**
 * @author andy
 * @since 2/17/20
 **/
case class ReviewNotRememberHandler(nlpService: NLPService,
                                    botConfig: BotConfig,
                                    analytics: Analytics,
                                    processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {

    for {
      (cardId, component) <- processor.getCurrentComponentToReview(context)
      isCompleted <- component match {
        case Some(_) => processor.sendQuestionMessage(context).map(_ => false)
        case _ => processor.submitAnswerAndUpdateScore(context, cardId, false)
      }
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
