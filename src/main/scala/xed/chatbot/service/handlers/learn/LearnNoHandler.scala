package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.api_service.util.Utils
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.ActionHandler

case class LearnNoHandler(nlpService: NLPService,
                          botConfig: BotConfig,
                          analytics: Analytics,
                          learnService: LearnService,
                          processor: LearnProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    for {
      learnCard <- processor.doLearnNo(context)
      learningInfo <- learnService.getLearningInfo(context.recipient).map(Utils.throwIfNotExist(_))
      hasCompletedAndConfirmRequired <- learnCard match {
        case Some(learnCard) =>
         processor.checkCourseSessionTopicCompleted(context,
            learningInfo,
            learnCard)
        case _ => Future.False
      }
      _ <- if (hasCompletedAndConfirmRequired) Future.Unit else {
        processor.beginLearn(context, learningInfo)
      }
    } yield {

    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = Future.Unit

}