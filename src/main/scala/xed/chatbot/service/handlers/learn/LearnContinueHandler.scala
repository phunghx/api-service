package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.domain.course.UserLearningInfo
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.ActionHandler

case class LearnContinueHandler(nlpService: NLPService,
                                botConfig: BotConfig,
                                analytics: Analytics,
                                learnService: LearnService,
                                processor: LearnProcessor) extends ActionHandler {


  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    learnService.getOrCreateLearningInfo(context.recipient).flatMap({
      case learningInfo if isInitRequired(learningInfo) =>
        processor.handleSelectCourseAndWaitFor(context, true, chatMessage)
      case learningInfo =>
        processor.beginLearn(context,learningInfo)

    })
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  Future.Unit

  private def isInitRequired(info: UserLearningInfo) = {
    info.isCourseRequired
  }
}