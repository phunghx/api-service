package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.domain.course.CourseInfo
import xed.api_service.service._
import xed.api_service.service.course.CourseService
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.ActionHandler

case class LearnHandler(nlpService: NLPService,
                        botConfig: BotConfig,
                        analytics: Analytics,
                        courseService: CourseService,
                        learnService: LearnService,
                        processor: LearnProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    detectSelectedCourse(context).flatMap(processor.setupContextAndStart(context, _))
  }


  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    for {
      learningInfo <- learnService.getOrCreateLearningInfo(context.recipient)
      r <- processor.handleCompletionCheck(context, learningInfo)
      _ <- if (r) {
        Future.Unit
      } else if (learningInfo.isCourseRequired) {
        processor.handleSelectCourseAndWaitFor(context, false, chatMessage)
      } else {
        processor.beginLearn(context, learningInfo)
      }
    } yield {

    }
  }


  private def detectSelectedCourse(context: BotContext): Future[Option[CourseInfo]] = {
    val courseId = context.actionInfo.getCourseIdParam()
    if(courseId.isEmpty) Future.None else {
      courseService.get(courseId.get)
    }
  }





}








