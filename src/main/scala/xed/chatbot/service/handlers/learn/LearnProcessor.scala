package xed.chatbot.service.handlers.learn

import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.course.{CourseInfo, JourneyInfo, LearnCard, UserLearningInfo}
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.exception.InternalError
import xed.api_service.domain.{Card, Deck}
import xed.api_service.service.course.{CourseService, JourneyService}
import xed.api_service.service.{ CardService, DeckService, NLPService, SRSService}
import xed.api_service.util.Utils
import xed.chatbot.domain._
import xed.chatbot.service.LearnService
import xed.chatbot.service.handlers.test.TestProcessor
import xed.chatbot.service.handlers.{ActionHandler, Processor}
import xed.profiler.Profiler

/**
 * @author andy
 * @since 2/18/20
 **/
case class LearnProcessor(nlpService: NLPService,
                          botConfig: BotConfig,
                          learnService: LearnService,
                          srsService: SRSService,
                          courseService: CourseService,
                          journeyService: JourneyService,
                          deckService: DeckService,
                          cardService: CardService,
                          testProcessor: TestProcessor) extends Processor {

  val actionType: String = IntentActionType.LEARN


  final val courseSelector = CourseSelector(
    nlpService,
    botConfig,
    learnService,
    courseService,
    this
  )

  def setupContextAndStart(context: BotContext, selectedCourseInfo: Option[CourseInfo]): Future[Unit] = {
    context.removeContextParam(BotContext.LEARN_TEST_CTX)
    context.removeContextParam(BotContext.LEARN_TEST_FOLLOWCTX)

    context.updateContextData(BotContext.LEARN_VOCABULARY_CTX, EmptyContextData())
    context.updateContextData(BotContext.LEARN_VOCABULARY_FOLLOWCTX, EmptyContextData())

    selectedCourseInfo match {
      case Some(courseInfo) => changeCourseAndWaitFor(context, courseInfo)
      case _ =>
        learnService.resetLearningInfo(context.recipient).flatMap(learningInfo => {
          handleSelectCourseAndWaitFor(context, true, null)
        })
    }
  }



  def beginLearn(context: BotContext, learningInfo: UserLearningInfo): Future[Unit] = {
    val learningCard = learningInfo.getLearningCardId()
    val courseId = learningCard.map(_.courseId)
      .flatMap(x => if(x == null || x.isEmpty) None else Some(x))
    val sectionId = learningCard.map(_.sectionId)
      .flatMap(x => if(x == null || x.isEmpty) None else Some(x))
    val topicId = learningCard.map(_.topicId)
      .flatMap(x => if(x == null || x.isEmpty) None else Some(x))

    for {
      isNewSection <- sectionId.fold(Future.False)(learnService.isNewSection(context.recipient,_))
      isNewTopic <- topicId.fold(Future.False)(learnService.isNewTopic(context.recipient,_))

      r <- if(isNewSection) {
        startSessionIntroductionPhrase(context,
          courseId.get,
          sectionId.get,
          if(isNewTopic) topicId else None)
      } else if(isNewTopic) {
        startTopicIntroductionPhrase(context,
          courseId.get,
          topicId.get)
      } else {
        testProcessor.setupContextAndStart(context, learningCard.map(_.cardId).toSeq)
      }
    } yield {

    }

  }

  def startSessionIntroductionPhrase(context: BotContext,
                                     courseId: String,
                                     sectionId: String,
                                     topicId: Option[String]): Future[Unit] = {
    for {
      _ <- learnService.markLearning( context.recipient,
        Some(courseId),
        Some(sectionId),
        topicId)
      courseInfo <- courseService.get(courseId)
      journeyInfo <- journeyService.get(sectionId).map(Utils.throwIfNotExist(_))
    } yield  {
      context.removeLearnContext()
      context.removeLearnTestContext()
      context.removeChallengeContextData()
      context.initLearnIntroductionContext(
        courseInfo = courseInfo,
        courseId = None,
        sectionId = Some(sectionId),
        topicId =  topicId
      )
      context.write(buildSectionIntroductionMessage(context, journeyInfo))
    }


  }

  def startTopicIntroductionPhrase(context: BotContext,
                                   courseId: String,
                                   topicId: String): Future[Unit] = {
    for {
      courseInfo <- courseService.get(courseId)
      deck <- deckService.getDeck(topicId)
      _ <- learnService.markLearning( context.recipient,
        Some(courseId),
        None,
        Option(topicId))

    } yield  {
      context.removeLearnContext()
      context.removeLearnTestContext()
      context.removeChallengeContextData()
      context.initLearnIntroductionContext(
        courseInfo = courseInfo,
        courseId = None,
        sectionId = None,
        topicId =  Some(topicId)
      )
      context.write(buildTopicIntroductionMessage(context, deck))
    }
  }

  /**
   * Check and collect course/section/topic need to show
   * Then setup a introduction flow and switch user to that flow.
   * @param context
   * @param course
   * @return
   */
  def startCourseIntroductionPhrase(context: BotContext, course: CourseInfo): Future[Unit] = {
    for {
      learningInfo <- learnService.getLearningInfo(context.recipient)
      learningCard  = learningInfo.flatMap(_.getLearningCardId())
      sectionId = learningCard.map(_.sectionId)
      topicId = learningCard.map(_.topicId)
      isNewSection <- sectionId.fold(Future.False)(learnService.isNewSection(context.recipient,_))
      isNewTopic <- topicId.fold(Future.False)(learnService.isNewTopic(context.recipient,_))

      _ <- learnService.markLearning( context.recipient,
        Some(course.id),
        sectionId,
        topicId)

    } yield  {
      context.removeLearnContext()
      context.removeLearnTestContext()
      context.removeChallengeContextData()
      context.initLearnIntroductionContext(
        courseInfo = Option(course),
        courseId = Some(course.id),
        sectionId = if(isNewSection) sectionId else None,
        topicId =  if(isNewTopic) topicId else None
      )
      context.write(buildCourseIntroductionMessage(context, course))
    }
  }


  def buildNoCourseMessage(context: BotContext): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getNoCourseMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.UNKNOWN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  def buildCourseIntroductionMessage(context: BotContext, course: CourseInfo): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getCourseIntroducedMessage(
            course.name.getOrElse(""),
            course.description.getOrElse("")
          ))
        ),
        actions = Seq(
          PostBackUAction("Ok", "Ok")
        ),
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo)
        .map(_.copy(actionType = IntentActionType.LEARN_INTRODUCTION)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  def buildSectionIntroductionMessage(context: BotContext, journeyInfo: JourneyInfo): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getSectionIntroducedMessage(
            journeyInfo.name.getOrElse(""),
            journeyInfo.description.getOrElse("")
          ))
        ),
        actions = Seq(
          PostBackUAction("Ok", "Ok")
        ),
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo)
        .map(_.copy(actionType = IntentActionType.LEARN_INTRODUCTION)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  def buildTopicIntroductionMessage(context: BotContext, deck: Deck): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getTopicIntroducedMessage(
            deck.name.getOrElse(""),
            deck.description.getOrElse("")
          ))
        ),
        actions = Seq(
          PostBackUAction("Ok", "Ok")
        ),
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo)
        .map(_.copy(actionType = IntentActionType.LEARN_INTRODUCTION)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }



  private def changeCourseAndWaitFor(context: BotContext,
                                     courseInfo: CourseInfo): Future[Unit] = Profiler(s"$clazz.changeCourseAndWaitFor") {

    for {
      isNewCourse <- learnService.isNewCourse(context.recipient, courseInfo.id)
      r <- learnService.changeCourse(context.recipient, courseInfo.id)

      _ <- if(isNewCourse) {
        startCourseIntroductionPhrase(context,courseInfo)
      } else {
        learnService.getOrCreateLearningInfo(context.recipient).flatMap(learnInfo => {
          beginLearn(context, learnInfo)
        })
      }
    } yield {
    }
  }


  def handleSelectCourseAndWaitFor(context: BotContext,
                                   isFirstTime: Boolean,
                                   chatMessage: ChatMessage): Future[Unit] = Profiler(s"$clazz.handleSelectCourseAndWaitFor") {


    val fn = if (isFirstTime)
      courseSelector.setupAndShowListing(context).map({
        case true => true
        case _ => throw InternalError(Some("No course was found"))
      })
    else
      courseSelector.processUserReply(context, chatMessage)

    fn.transform({
      case Return(r) => Future.Unit
      case _ =>
        context.removeLearnContext()
        context.removeLearnIntroductionContext()
        context.removeLearnTestContext()
        context.write(buildNoCourseMessage(context))
        context.write(ActionHandler.buildHelpMessage(context, botConfig))
        Future.Unit
    })
  }




  /**
   *
   * @param context
   * @return stop learning if true and vice verse
   */
  def handleCompletionCheck(context: BotContext,
                            learningInfo: UserLearningInfo) : Future[Boolean] = {
    for {
      courseId <- learnService.courseCompletionCheck(context.recipient, learningInfo)
      r <- courseId match {
        case Some(courseId) =>
          handleCourseCompleted(context, learningInfo, courseId).map(_ => true)
        case _ => Future.False
      }
    } yield {
      r
    }
  }

  def checkCourseSessionTopicCompleted(context: BotContext,
                                       learningInfo: UserLearningInfo,
                                       learnCard: LearnCard) : Future[Boolean] = {

    for {
      progressInfo <- learnService.completionCheck(context.recipient,
        learningInfo,
        learnCard.courseId,
        learnCard.sectionId,
        learnCard.topicId
      )
      r <- if (progressInfo.completedCourseId.isDefined) {
        handleCourseCompleted(context,
          learningInfo,
          progressInfo.completedCourseId.get).map(_ => true)
      } else if (progressInfo.completedTopicId.isDefined) {
        handleTopicCompleted(context,
          learningInfo,
          progressInfo.completedTopicId.get).map(_ => true)
      } else
        Future.False
    } yield {
      r
    }
  }



  def handleCourseCompleted(context: BotContext,
                            learningInfo: UserLearningInfo,
                            courseId: String): Future[Unit] =  {
    for {
      course <- courseService.get(courseId).map({
        case Some(x) => x
        case _ => CourseInfo(
          id = courseId,
          level = None,
          name = Some("[This course was removed]"),
          thumbnail = None,
          description = None,
          journeyIds = None,
          deckIds = None,
          totalCard = None,
          status = None,
          creator = None,
          updatedTime = Some(System.currentTimeMillis()),
          createdTime = Some(System.currentTimeMillis())
        )
      })

      _ <- learnService.addLearningInfo(learningInfo.copy(
        courseId = None,
        cardIds = None
      ))
    } yield {
      context.write(buildCourseCompletedMessage(context, course))
    }

  }

  def handleTopicCompleted(context: BotContext,
                           learningInfo: UserLearningInfo,
                           topicId: String): Future[Unit] =  {
    for {
      deck <- deckService.getDeck(topicId).transform({
        case Return(r) => Future.value(r)
        case Throw(e) =>  Future.value(Deck(
          id = topicId,
          name = Some("[This topic was removed]"),
          username =Option( context.recipient.username),
          thumbnail = None,
          design = None,
          description = None,
          cards = None,
          updatedTime = Some(System.currentTimeMillis()),
          createdTime = Some(System.currentTimeMillis())
        ))
      })
    } yield {
      context.write(buildTopicCompletedMessage(context, deck))
    }

  }


  def handleShowVocabulary(context: BotContext, learningInfo: UserLearningInfo): Future[Unit] = {
    showVocabularyCard(context, learningInfo).map(_ => {})
  }


  def showVocabularyCard(context: BotContext, learningInfo: UserLearningInfo): Future[Boolean] = {

    val maybeCardId = learningInfo.getLearningCardId().map(_.cardId)
    maybeCardId match {
      case Some(cardId) => sendBackCard(context, cardId)
      case _ => Future.False
    }
  }

  def doLearnYes(context: BotContext): Future[Option[LearnCard]] = {
    for {
      learningInfo <- learnService.getLearningInfo(context.recipient).map(Utils.throwIfNotExist(_))
      learnCard = learningInfo.getLearningCardId()

      reviewOK <- if (learnCard.isEmpty) Future.False else {
        learnService.learnCard(context.recipient, learnCard.get)
      }
      r <- if (reviewOK) {
        learnService.addLearningInfo(learningInfo.removeCurrentAndNext(false))
      } else Future.False
    } yield {
      if (r) learnCard
      else None
    }
  }

  def doLearnNo(context: BotContext): Future[Option[LearnCard]] = {

    for {
      learningInfo <- learnService.getLearningInfo(context.recipient).map(Utils.throwIfNotExist(_))
      learnCard = learningInfo.getLearningCardId()

      reviewOK <- if (learnCard.isEmpty) Future.False else {
        learnService.ignoreCard(context.recipient, learnCard.get)
      }
      r <- if (reviewOK) {
        learnService.addLearningInfo(learningInfo.removeCurrentAndNext(true))
      } else Future.False
    } yield {
      if (r) learnCard
      else None
    }
  }


  def sendBackCard(context: BotContext, cardId: String): Future[Boolean] = {

    cardService.getCard(cardId).map(card => {
      context.write(buildBackCardMessage(context, card))
      true
    })
  }


  def buildBackCardMessage(context: BotContext,
                           card: Card,
                           enableSuggestion: Boolean = true): ChatMessage = {


    val backCard = card.backCard()

    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          backCard
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = if (enableSuggestion) Seq(
          PostBackUAction(botConfig.getLearnYesAction().title, botConfig.getLearnYesAction().value),
          PostBackUAction(botConfig.getLearnNoAction().title, botConfig.getLearnNoAction().value),
          PostBackUAction("Exit", "Exit")
        )
        else Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.LEARN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

  }



  def buildCourseCompletedMessage(context: BotContext, course: CourseInfo): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getCourseCompletedMessage(course.name.getOrElse("")))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq(
          PostBackUAction(botConfig.getReviewAction().title, botConfig.getReviewAction().value),
          PostBackUAction(botConfig.getLearnAction().title, botConfig.getLearnAction().value),
          PostBackUAction("Exit", "Exit")
        )
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.LEARN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }


  def buildTopicCompletedMessage(context: BotContext,
                                 deck: Deck): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getTopicCompletedMessage(deck.name.getOrElse("")))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq(
          PostBackUAction("Ok", "Ok")
        )
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.LEARN)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }





  def sendCourseCompletedMessage(context: BotContext, maybeCourse: Option[CourseInfo]): Future[Unit] = Future {
    maybeCourse.map(course => {
      context.write(buildCourseCompletedMessage(context, course))
    })
  }


}
