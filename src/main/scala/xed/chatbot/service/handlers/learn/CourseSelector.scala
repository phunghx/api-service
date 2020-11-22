package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.domain.course.CourseInfo
import xed.api_service.domain.design.v100.{Answer, MultiChoice, Text}
import xed.api_service.service.NLPService
import xed.api_service.service.course.CourseService
import xed.api_service.util.Utils
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.BaseItemSelector

import scala.collection.mutable.ListBuffer

case class CourseSelector(nlpService: NLPService,
                          botConfig: BotConfig,
                          learnService: LearnService,
                          courseService: CourseService,
                          learnProcessor: LearnProcessor) extends BaseItemSelector {

  override protected def onShowListing(context: BotContext): Future[Boolean] = {

    val pagingData = context.getCoursePagingParam()
    for {
      r <- courseService.getAvailableCourses(
        from = pagingData.from.getOrElse(0.0).toInt,
        size = botConfig.listingItemSize
      )
      courseInfos = r.records
      courseIds = courseInfos.map(_.id)
      totalCourseCount = r.total.toInt
    } yield {
      context.updateCoursePagingParam(PagingData(
        totalItemCount = totalCourseCount,
        from = if (r.records.isEmpty) Some(0) else pagingData.from,
        size = Some(botConfig.listingItemSize),
        ids = Option(courseIds)
      ))
      if (courseIds.nonEmpty)
        context.write(buildListingMsg(context, courseInfos))
      courseIds.nonEmpty
    }
  }

  override protected def onPrevPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getCoursePagingParam()
    for {
      ok <- Future.value(prevPage(context))
      courseInfos <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) - botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        courseService.getAvailableCourses(
          from = pagingData.from.getOrElse(0.0).toInt,
          size = botConfig.listingItemSize
        ).map(_.records)
      } else Future.value(Seq.empty)
      courseIds = courseInfos.map(_.id)
    } yield {
      pagingData = pagingData.copy(ids = Some(courseIds))
      context.updateCoursePagingParam(pagingData)
      context.write(buildListingMsg(context, courseInfos))
    }
  }

  override protected def onNextPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getCoursePagingParam()
    for {
      ok <- Future.value(nextPage(context))
      courseInfos <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) + botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        courseService.getAvailableCourses(
          from = pagingData.from.getOrElse(0.0).toInt,
          size = botConfig.listingItemSize
        ).map(_.records)
      } else Future.value(Seq.empty)
      courseIds = courseInfos.map(_.id)
    } yield {
      pagingData = pagingData.copy(ids = Some(courseIds))
      context.updateCoursePagingParam(pagingData)
      context.write(buildListingMsg(context, courseInfos))
    }
  }

  private def buildListingMsg(context: BotContext, items: Seq[CourseInfo]): ChatMessage = {
    val question = MultiChoice(
      question = botConfig.getCourseListingMsg(),
      answers = items.foldLeft(ListBuffer.empty[Answer])((answers, item) => {
        answers.append(Answer(
          text = item.name
        ))
        answers
      })
    )

    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(question),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = getActions(context)
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.LEARN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  private def prevPage(context: BotContext): Boolean = {
    val param = context.getCoursePagingParam()
    if (param.canPaging(false)) {
      context.updateCoursePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    }else false

  }

  private def nextPage(context: BotContext): Boolean = {
    val param = context.getCoursePagingParam()
    if (param.canPaging(true)) {
      context.updateCoursePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    }else false

  }

  override protected def onItemSelected(context: BotContext, indices: Seq[Int]): Future[Unit] = {

    def getSelectedCourse() = {
      val param = context.getCoursePagingParam()
      val itemId = param.ids.getOrElse(Seq.empty) lift indices.head
      itemId.fold[Future[Option[CourseInfo]]](Future.None)(courseService.get(_))
    }


    for {
      course <- getSelectedCourse().map(Utils.throwIfNotExist(_, Some("This course is no longer exist.")))
      isNewCourse <- learnService.isNewCourse(context.recipient, course.id)
      isCompleted <- learnService.isCourseCompleted(context.recipient, course.id)
      addOk <- if (isCompleted) {
        learnProcessor.sendCourseCompletedMessage(context, Some(course)).map(_ => false)
      } else {
        learnService.changeCourse(context.recipient, course.id).map(_.isDefined)
      }
      _ <- if (!addOk) Future.Unit else {
        if (isNewCourse) {
          learnProcessor.startCourseIntroductionPhrase(context, course)
        } else {
          learnService.getOrCreateLearningInfo(context.recipient).flatMap(learnProcessor.beginLearn(context, _))
        }
      }
    } yield {
    }
  }

  override def getActions(context: BotContext): Seq[UserAction] = {
    val param = context.getCoursePagingParam()

    val actions = ListBuffer.empty[UserAction]
    if (param.canPaging(false))
      actions.append(PostBackUAction(botConfig.getPrevPageAction().title, botConfig.getPrevPageAction().value))

    if (param.canPaging(true))
      actions.append(PostBackUAction(botConfig.getNextPageAction().title, botConfig.getNextPageAction().value))
    actions
  }

  override def getSuggestionActions(context: BotContext): Seq[UserAction] = {
    val actions = ListBuffer.empty[UserAction]
    actions
  }

  override protected def buildUnknownCommandMessage(context: BotContext): Seq[ChatMessage] = {
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getUnrecognizedMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Some(context.recipient.username)
    )
    Seq(msg)
  }
}

