package xed.chatbot.service.handlers.challenge

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain.{BotConfig, BotContext, ChatMessage}
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor

/**
 * @author andy
 * @since 2/17/20
 **/
case class ChallengeNotRememberHandler(nlpService: NLPService,
                                       botConfig: BotConfig,
                                       analytics: Analytics,
                                       processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {

    val challengeData = context.getChallengeContextData()
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
      context.updateChallengeContext(challengeData)
    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

}
