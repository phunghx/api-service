package xed.api_service.module

import akka.actor.{ActorRef, ActorSystem, Props}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.cloud.dialogflow.v2beta1.Context
import com.google.inject.name.Named
import com.google.inject.{Inject, Provides, Singleton}
import com.twitter.inject.TwitterModule
import xed.api_service.service.course.{CourseService, JourneyService}
import xed.api_service.service.{MessageService => _, _}
import xed.api_service.util._
import xed.chatbot._
import xed.chatbot.domain.{BotConfig, IntentActionType}
import xed.chatbot.service.handlers.learn._
import xed.chatbot.service.handlers.learn_introduction.LearnIntroductionHandler
import xed.chatbot.service.handlers.review._
import xed.chatbot.service.handlers.test._
import xed.chatbot.service.handlers.{review, test, _}
import xed.chatbot.service.{LearnService, _}
import xed.userprofile.UserProfileService

object CustomJsonModule extends SimpleModule {
  addDeserializer(classOf[Context],new GDialogFlowContextDeserializer)
  addSerializer(classOf[Context],new GDialogFlowContextToJsonSerializer)
}

object HandlerModule extends HandlerModule {

}

class HandlerModule extends TwitterModule {

  override def configure(): Unit = {
    super.configure()

  }


  @Provides
  @Singleton
  def providesKikiBotService(@Inject nlpService: NLPService,
                             botConfig: BotConfig,
                             analytics: Analytics,
                             resolveActionEngine: ActionResolveEngine,
                             handlers: Map[String, ActionHandler],
                             @Named("forward_handler") defaultHandler: ActionHandler,
                             @Named("fallback_handler") fallbackHandler: ActionHandler,
                             @Named("error_handler") errorHandler: ActionHandler,
                             @Named("conversation_start_handler") conversationStartHandler: ActionHandler
                            ): BotService = {
    KiKiBotServiceImpl(
      resolveActionEngine,
      nlpService,
      botConfig,
      analytics,
      handlers,
      conversationStartHandler,
      defaultHandler,
      fallbackHandler,
      errorHandler
    )
  }


  @Provides
  @Singleton
  @Named("kiki-message-actor")
  def providesChatBotMessageActor(@Inject botService: BotService,
                                  messageService: MessageService,
                                  system: ActorSystem
                                 ): ActorRef = {
    system.actorOf(Props(
      classOf[MessageEventProcessor],
      botService,
      messageService
    ), "kiki-message-actor")
  }




  @Provides
  @Singleton
  def providesActionHandlers(@Inject
                             @Named("forward_handler") forwardHandler: ActionHandler,
                             @Named("fallback_handler") fallbackHandler: ActionHandler,
                             @Named("help_handler") helpHandler: ActionHandler,
                             @Named("conversation_start_handler") conversationStartHandler: ActionHandler,
                             @Named("learn_handler") learnHandler: ActionHandler,
                             @Named("learn_introduction_handler") learnIntroductionHandler: ActionHandler,
                             @Named("learn_yes_handler") learnYesHandler: ActionHandler,
                             @Named("learn_no_handler") learnNoHandler: ActionHandler,
                             @Named("learn_test_handler") learnTestHandler: ActionHandler,
                             @Named("test_known_question_handler") testKnownHandler: ActionHandler,
                             @Named("test_dont_known_question_handler") testDontKnownHandler: ActionHandler,
                             @Named("test_exit_handler") testExitHandler: ActionHandler,
                             @Named("learn_continue_handler") learnContinueHandler: ActionHandler,
                             @Named("learn_stop_handler") learnStopHandler: ActionHandler,
                             @Named("review_handler") reviewHandler: ActionHandler,
                             @Named("review_known_question_handler") reviewKnownHandler: ActionHandler,
                             @Named("review_dont_known_question_handler") reviewDontKnownHandler: ActionHandler,
                             @Named("review_continue_handler") reviewContinueHandler: ActionHandler,
                             @Named("review_skip_handler") reviewSkipHandler: ActionHandler,
                             @Named("review_exit_handler") reviewExitHandler: ActionHandler,

                             @Named("challenge_create_handler") challengeCreateHandler: ActionHandler,
                             @Named("challenge_join_handler") challengeJoinHandler: ActionHandler,
                             @Named("challenge_play_handler") challengePlayHandler: ActionHandler,
                             @Named("challenge_known_question_handler") challengeKnownHandler: ActionHandler,
                             @Named("challenge_dont_known_question_handler") challengeDontKnownHandler: ActionHandler,
                             @Named("challenge_continue_handler") challengeContinueHandler: ActionHandler,
                             @Named("challenge_submit_handler") challengeSubmitHandler: ActionHandler,

                             @Named("dict_search_handler") dictSearchHandler: ActionHandler,
                             @Named("dict_exit_handler") dictExitHandler: ActionHandler): Map[String, ActionHandler] = {
    Map(
      IntentActionType.HELP -> helpHandler,
      IntentActionType.LEARN -> learnHandler,
      IntentActionType.LEARN_INTRODUCTION -> learnIntroductionHandler,
      IntentActionType.LEARN_YES -> learnYesHandler,
      IntentActionType.LEARN_NO -> learnNoHandler,
      IntentActionType.LEARN_TEST -> learnTestHandler,
      IntentActionType.LEARN_TEST_KNOWN_QUESTION -> testKnownHandler,
      IntentActionType.LEARN_TEST_DONT_KNOW_QUESTION -> testDontKnownHandler,
      IntentActionType.LEARN_TEST_EXIT -> testExitHandler,
      IntentActionType.LEARN_CONTINUE -> learnContinueHandler,
      IntentActionType.LEARN_STOP -> learnStopHandler,
      IntentActionType.REVIEW -> reviewHandler,
      IntentActionType.REVIEW_KNOWN_QUESTION -> reviewKnownHandler,
      IntentActionType.REVIEW_DONT_KNOW_QUESTION -> reviewDontKnownHandler,
      IntentActionType.REVIEW_CONTINUE -> reviewContinueHandler,
      IntentActionType.REVIEW_SKIP -> reviewSkipHandler,
      IntentActionType.REVIEW_STOP -> reviewExitHandler,

      IntentActionType.CHALLENGE_CREATE -> challengeCreateHandler,
      IntentActionType.CHALLENGE_JOIN -> challengeJoinHandler,
      IntentActionType.CHALLENGE_PLAY -> challengePlayHandler,
      IntentActionType.CHALLENGE_KNOWN_QUESTION -> challengeKnownHandler,
      IntentActionType.CHALLENGE_DONT_KNOWN_QUESTION -> challengeDontKnownHandler,
      IntentActionType.CHALLENGE_CONTINUE -> challengeContinueHandler,
      IntentActionType.CHALLENGE_SUBMIT -> challengeSubmitHandler,

      IntentActionType.DICT_SEARCH -> dictSearchHandler,
      IntentActionType.DICT_EXIT -> dictExitHandler
    )
  }

  @Provides
  @Singleton
  @Named("fallback_handler")
  def providesFallbackActionHandler(@Inject botConfig: BotConfig,
                                    nlpService: NLPService,
                                    analytics: Analytics,
                                    processors: Seq[TryProcessor]): ActionHandler = {
    FallbackHandler(
      nlpService,
      botConfig,
      analytics,
      processors
    )
  }

  @Provides
  @Singleton
  @Named("error_handler")
  def providesErrorHandler(@Inject botConfig: BotConfig,
                           nlpService: NLPService,
                           analytics: Analytics): ActionHandler = {
    ErrorHandler(nlpService, botConfig, analytics)
  }

  @Provides
  @Singleton
  @Named("forward_handler")
  def providesForwardHandler(@Inject botConfig: BotConfig,
                             nlpService: NLPService,
                             analytics: Analytics): ActionHandler = {
    ForwardHandler(nlpService, botConfig, analytics)
  }


  @Provides
  @Singleton
  @Named("help_handler")
  def providesHelpHandler(@Inject botConfig: BotConfig,
                          nlpService: NLPService,
                          analytics: Analytics): ActionHandler = {
    HelpHandler(nlpService, botConfig, analytics)
  }

  @Provides
  @Singleton
  @Named("dict_processor")
  def providesDictionaryProcessor(@Inject botConfig: BotConfig,
                                dictionaryService: DictionaryService,
                                profileService: UserProfileService): DictionaryProcessor = {
    DictionaryProcessor(
      botConfig,
      dictionaryService,
      profileService
    )
  }

  @Provides
  @Singleton
  @Named("dict_search_handler")
  def providesDictSearchHandler(@Inject botConfig: BotConfig,
                                nlpService: NLPService,
                                analytics: Analytics,
                                @Named("dict_processor") processor: DictionaryProcessor): ActionHandler = {
    DictionarySearchHandler(nlpService,
      botConfig,
      analytics,
      processor)
  }

  @Provides
  @Singleton
  @Named("dict_exit_handler")
  def providesDictExitHandler(@Inject botConfig: BotConfig,
                              nlpService: NLPService,
                              analytics: Analytics,
                              @Named("dict_processor") processor: DictionaryProcessor): ActionHandler = {
    DictionaryExitHandler(nlpService,
      botConfig,
      analytics,
      processor)
  }

  @Provides
  @Singleton
  @Named("review_processor")
  def providesReviewProcessor(@Inject botConfig: BotConfig,
                            srsService: SRSService,
                            cardService: CardService): TestProcessor = {

    TestReviewProcessor(botConfig, srsService, cardService)
  }

  @Provides
  @Singleton
  @Named("review_handler")
  def providesReviewHandler(@Inject botConfig: BotConfig,
                            nlpService: NLPService,
                            analytics: Analytics,
                           @Named("review_processor") processor: TestProcessor ): ActionHandler = {
    ReviewHandler(
      nlpService,
      botConfig,
      analytics,
      processor,
      FIBHandler(
        botConfig,
        processor
      ),
      MCHandler(
        botConfig,
        processor
      )
    )
  }

  @Provides
  @Singleton
  @Named("review_continue_handler")
  def providesReviewContinueHandler(@Inject botConfig: BotConfig,
                                nlpService: NLPService,
                                    analytics: Analytics,
                                @Named("review_processor") processor: TestProcessor): ActionHandler = {
    ReviewContinueHandler(nlpService,
      botConfig,
      analytics,
      processor
    )
  }

  @Provides
  @Singleton
  @Named("review_skip_handler")
  def providesReviewSkipHandler(@Inject botConfig: BotConfig,
                                nlpService: NLPService,
                                analytics: Analytics,
                                @Named("review_processor") processor: TestProcessor): ActionHandler = {
    ReviewSkipHandler(nlpService,
      botConfig,
      analytics,
      processor
    )
  }

  @Provides
  @Singleton
  @Named("review_exit_handler")
  def providesReviewExitHandler(@Inject botConfig: BotConfig,
                                nlpService: NLPService,
                                analytics: Analytics,
                                @Named("review_processor") processor: TestProcessor): ActionHandler = {
    ReviewExitHandler(nlpService,
      botConfig,
      analytics,
      processor
    )
  }

  @Provides
  @Singleton
  @Named("review_known_question_handler")
  def providesReviewKnownQuestionHandler(@Inject botConfig: BotConfig,
                                         nlpService: NLPService,
                                         analytics: Analytics,
                                         @Named("review_processor") processor: TestProcessor): ActionHandler = {
    review.ReviewRememberHandler(nlpService,
      botConfig,
      analytics,
      processor
    )
  }

  @Provides
  @Singleton
  @Named("review_dont_known_question_handler")
  def providesReviewDontKnownQuestionHandler(@Inject botConfig: BotConfig,
                                             nlpService: NLPService,
                                             analytics: Analytics,
                                             @Named("review_processor") processor: TestProcessor): ActionHandler = {
    review.ReviewNotRememberHandler(nlpService,
      botConfig,
      analytics,
      processor
    )
  }

  @Provides
  @Singleton
  @Named("conversation_start_handler")
  def providesConversationStartHandler(@Inject botConfig: BotConfig,
                                       nlpService: NLPService,
                                       analytics: Analytics,
                                       srsService: SRSService,
                                       profileService: UserProfileService
                                      ): ActionHandler = {
    handlers.ConversationStartHandler(nlpService,
      botConfig,
      analytics,
      srsService,
      profileService
    )
  }

  @Provides
  @Singleton
  @Named("learn_processor")
  def providesLearnProcessor(@Inject
                                     nlpService: NLPService,
                                     botConfig: BotConfig,
                                     srsService: SRSService,
                                     courseService: CourseService,
                                     journeyService: JourneyService,
                                     deckService: DeckService,
                                     cardService: CardService,
                                     learnService: LearnService,
                                     @Named("learn_test_processor") testProcessor: TestProcessor): LearnProcessor = {
    LearnProcessor(nlpService,
      botConfig,
      learnService,
      srsService,
      courseService,
      journeyService,
      deckService,
      cardService,
      testProcessor
    )
  }

  @Provides
  @Singleton
  @Named("learn_handler")
  def providesLearnVocabularyHandler(@Inject
                                     @Named("learn_processor") learnProcessor: LearnProcessor,
                                     courseService: CourseService,
                                     learnService: LearnService,
                                     analytics: Analytics,
                                     nlpService: NLPService,
                                     botConfig: BotConfig): ActionHandler = {
    LearnHandler(nlpService,
      botConfig,
      analytics,
      courseService,
      learnService,
      learnProcessor
    )
  }

  @Provides
  @Singleton
  @Named("learn_introduction_handler")
  def providesLearnIntroductionHandler(@Inject
                                     @Named("learn_processor") learnProcessor: LearnProcessor,
                                     courseService: CourseService,
                                       journeyService: JourneyService,
                                       deckService: DeckService,
                                     learnService: LearnService,
                                       analytics: Analytics,
                                     nlpService: NLPService,
                                     botConfig: BotConfig): ActionHandler = {
    LearnIntroductionHandler(nlpService,
      botConfig,
      analytics,
      courseService,
      journeyService,
      deckService,
      learnService,
      learnProcessor
    )
  }


  @Provides
  @Singleton
  @Named("learn_yes_handler")
  def providesLearnYesHandler(@Inject
                              learnService: LearnService,
                              @Named("learn_processor") learnProcessor: LearnProcessor,
                              analytics: Analytics,
                              nlpService: NLPService,
                              botConfig: BotConfig): ActionHandler = {
    LearnYesHandler(nlpService,
      botConfig,
      analytics,
      learnService,
      learnProcessor
    )
  }

  @Provides
  @Singleton
  @Named("learn_no_handler")
  def providesLearnNoHandler(@Inject
                             learnService: LearnService,
                             @Named("learn_processor") learnProcessor: LearnProcessor,
                             analytics: Analytics,
                             nlpService: NLPService,
                             botConfig: BotConfig): ActionHandler = {
    LearnNoHandler(nlpService,
      botConfig,
      analytics,
      learnService,
      learnProcessor
    )
  }


  @Provides
  @Singleton
  @Named("learn_test_processor")
  def providesTestLearnProcessor(@Inject botConfig: BotConfig,
                              srsService: SRSService,
                              cardService: CardService): TestProcessor = {

    TestLearnProcessor(botConfig,
      srsService,
      cardService)
  }

  @Provides
  @Singleton
  @Named("learn_test_handler")
  def providesLearnTestHandler(@Inject botConfig: BotConfig,
                               nlpService: NLPService,
                               analytics: Analytics,
                               @Named("learn_test_processor") testProcessor: TestProcessor): ActionHandler = {

    val fibQuestionHandler = test.FIBHandler(
      botConfig,
      testProcessor
    )

    val mcQuestionHandler = test.MCHandler(botConfig,
      testProcessor
    )

    LearnTestHandler(nlpService,
      botConfig,
      analytics,
      testProcessor,
      fibQuestionHandler,
      mcQuestionHandler)
  }

  @Provides
  @Singleton
  @Named("test_known_question_handler")
  def providesTestKnownQuestionHandler(@Inject botConfig: BotConfig,
                                       nlpService: NLPService,
                                       analytics: Analytics,
                                       @Named("learn_test_processor") testProcessor: TestProcessor
                                      ): ActionHandler = {
    learn.LearnTestRememberHandler(nlpService,
      botConfig,
      analytics,
      testProcessor
    )
  }

  @Provides
  @Singleton
  @Named("test_dont_known_question_handler")
  def providesTestDontKnownQuestionHandler(@Inject botConfig: BotConfig,
                                           nlpService: NLPService,
                                           analytics: Analytics,
                                           @Named("learn_test_processor") testProcessor: TestProcessor
                                          ): ActionHandler = {
    learn.LearnTestNotRememberHandler(
      nlpService,
      botConfig,
      analytics,
      testProcessor)
  }

  @Provides
  @Singleton
  @Named("test_exit_handler")
  def providesTestExitHandler(@Inject botConfig: BotConfig,
                                       nlpService: NLPService,
                              analytics: Analytics,
                                       @Named("learn_processor") learnProcessor: LearnProcessor
                                      ): ActionHandler = {
    learn.LearnTestExitHandler(nlpService,
      botConfig,
      analytics,
      learnProcessor)
  }

  @Provides
  @Singleton
  @Named("learn_continue_handler")
  def providesLearnContinueHandler(@Inject
                                   @Named("learn_processor") learnProcessor: LearnProcessor,
                                   learnService: LearnService,
                                   analytics: Analytics,
                                   nlpService: NLPService,
                                   botConfig: BotConfig): ActionHandler = {
    LearnContinueHandler(nlpService,
      botConfig,
      analytics,
      learnService,
      learnProcessor
    )
  }


  @Provides
  @Singleton
  @Named("learn_stop_handler")
  def providesLearnResetHandler(@Inject
                                @Named("learn_processor") learnProcessor: LearnProcessor,
                                analytics: Analytics,
                                nlpService: NLPService,
                                botConfig: BotConfig): ActionHandler = {
    LearnStopHandler(
      nlpService,
      botConfig,
      analytics,
      learnProcessor
    )
  }

}

