package xed.chatbot.service.handlers.learn_introduction

import com.twitter.util.Future
import xed.api_service.service._
import xed.api_service.service.course.{CourseService, JourneyService}
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.learn.LearnProcessor

case class LearnIntroductionHandler(nlpService: NLPService,
                                    botConfig: BotConfig,
                                    analytics: Analytics,
                                    courseService: CourseService,
                                    journeyService: JourneyService,
                                    deckService: DeckService,
                                    learnService: LearnService,
                                    processor: LearnProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = Future {

  }




  override def handleUserReply(context: BotContext,
                               chatMessage: ChatMessage): Future[Unit] =  {
    val introductionData = context.getLearnIntroductionContextData()
    val courseId: Option[String] = introductionData.courseId
    val sectionId: Option[String] = introductionData.sectionId
    val topicId: Option[String] = introductionData.topicId

    if (courseId.isDefined) {
      proccessSectionOrTopicIntroduction(context, introductionData)
    } else if (sectionId.isDefined) {
      proccessTopicIntroduction(context, introductionData)
    } else {
      context.removeLearnIntroductionContext()
      learnService.markLearning(context.recipient,
        courseId,
        sectionId,
        topicId).flatMap(_ => processor.setupContextAndStart(context, introductionData.courseInfo))
    }
  }


  def proccessSectionOrTopicIntroduction(context: BotContext, data: LearnIntroductionData): Future[Unit] = {
    val courseId: Option[String] = data.courseId
    val sectionId: Option[String] = data.sectionId
    val topicId: Option[String] = data.topicId

    if(sectionId.isDefined) {
      proccessSectionIntroduction(context, data)
    }else if(topicId.isDefined) {
      proccessTopicIntroduction(context, data)
    } else {
      context.removeLearnIntroductionContext()
      learnService.markLearning(context.recipient,
        courseId,
        sectionId,
        topicId).flatMap(_ => processor.setupContextAndStart(context, data.courseInfo))
    }

  }

  def proccessSectionIntroduction(context: BotContext, data: LearnIntroductionData): Future[Unit] = {
    val courseId: Option[String] = data.courseId
    val sectionId: Option[String] = data.sectionId
    val topicId: Option[String] = data.topicId

    if(sectionId.isDefined) {
      val introductionData = data.copy(
        courseId = None,
        sectionId = sectionId,
        topicId = topicId
      )
      context.updateContextData(BotContext.LEARN_INTRODUCTION_CTX, introductionData)
      context.updateContextData(BotContext.LEARN_INTRODUCTION_FOLLOWCTX, EmptyContextData())
      journeyService.get(sectionId.get)
        .map(_.map(journey =>  processor.buildSectionIntroductionMessage(context, journey)))
        .map(_.map(msg => context.write(msg)))
        .flatMap({
          case Some(_) =>
            learnService.markLearning(
              context.recipient,
              courseId,
              None,
              None).map(_ => Unit)
          case _ =>
            proccessTopicIntroduction(context, introductionData.copy(
              sectionId = None
            ))
        })
    }else {
      proccessTopicIntroduction(context, data)
    }

  }


  def proccessTopicIntroduction(context: BotContext, data: LearnIntroductionData): Future[Unit] = {
    val courseId: Option[String] = data.courseId
    val sectionId: Option[String] = data.sectionId
    val topicId: Option[String] = data.topicId

    topicId match {
      case Some(topicId) =>
        val introductionData = data.copy(
          courseId = None,
          sectionId = None,
          topicId = Some(topicId)
        )
        context.updateContextData(BotContext.LEARN_INTRODUCTION_CTX, introductionData)
        context.updateContextData(BotContext.LEARN_INTRODUCTION_FOLLOWCTX, EmptyContextData())
        deckService.getDeck(topicId)
          .map(deck => processor.buildTopicIntroductionMessage(context, deck))
          .map(msg => context.write(msg))
          .flatMap(_ => learnService.markLearning(
            context.recipient,
            courseId,
            sectionId,
            None)).map(_ => {})

      case _ =>
        context.removeLearnIntroductionContext()
        learnService.markLearning(context.recipient,
          courseId,
          sectionId,
          topicId).flatMap(_ => processor.setupContextAndStart(context, data.courseInfo))
    }

  }

}








