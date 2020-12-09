package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor


case class LearnTestNotRememberHandler(nlpService: NLPService,
                                       botConfig: BotConfig,
                                       analytics: Analytics,
                                       processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {

    for {
      (cardId, component) <- processor.getCurrentComponentToReview(context)
      isCompleted <- component match {
        case Some(_) => processor.sendQuestionMessage(context).map(_ => false)
        case _ =>
          processor.submitAnswerAndUpdateScore(context, cardId, false)
      }
      _ <- if (!isCompleted) Future.Unit else {
        processor.onQuestionCompleted(context, cardId)
      }
    } yield {
      context.updateContextData(BotContext.LEARN_TEST_CTX,EmptyContextData())
    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

}
