package xed.api_service.module

import com.google.inject.name.Named
import com.google.inject.{Inject, Provides, Singleton}
import com.twitter.finagle.thrift
import com.twitter.inject.{Injector, TwitterModule}
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.service.course.CourseService
import xed.api_service.service.{MessageService => _, _}
import xed.api_service.util._
import xed.chatbot.domain.BotConfig
import xed.chatbot.repository.{ChallengeHistoryRepository, ChallengeRepository, ChallengeWatchList, ChallengeWatchListImpl}
import xed.chatbot.service.handlers.challenge.{ChallengeCreateHandler, ChallengeFIBHandler, ChallengeJoinHandler, ChallengeMCHandler, ChallengePlayHandler}
import xed.chatbot.service.handlers.test._
import xed.chatbot.service.handlers.{test, _}
import xed.chatbot.service.{ChallengeTemplateService, _}
import xed.leaderboard_mw.service.TLeaderBoardMWService
import xed.userprofile.UserProfileService



object ChallengeModule extends ChallengeModule {

}

class ChallengeModule extends TwitterModule {

  override def configure(): Unit = {
    super.configure()

  }

  override def singletonPostWarmupComplete(injector : Injector): Unit = {
    val watcher = injector.instance[ChallengeWatcher]
    watcher.start()
  }


  @Singleton
  @Provides
  def providesLeaderBoardService(@Inject userProfileService: UserProfileService ): LeaderBoardService = {
    import com.twitter.conversions.time._
    import com.twitter.finagle.Thrift
    import com.twitter.finagle.service.{Backoff, RetryBudget}
    import com.twitter.util.Duration

    val host = ZConfig.getString("leader_board.thrift.host")
    val port = ZConfig.getInt("leader_board.thrift.port")
    val timeoutInSecs = ZConfig.getInt("leader_board.thrift.timeout_in_secs", 5)
    val label = "leader_board-from-api-service"

    val client = Thrift.client
      .withRequestTimeout(Duration.fromSeconds(timeoutInSecs))
      .withSessionPool.minSize(1)
      .withSessionPool.maxSize(10)
      .withRetryBudget(RetryBudget())
      .withRetryBackoff(Backoff.exponentialJittered(5.seconds, 32.seconds))
      .withClientId(thrift.ClientId(label))
      .build[TLeaderBoardMWService.MethodPerEndpoint](s"$host:$port", label)

    LeaderBoardServiceImpl(client, userProfileService)
  }



  @Singleton
  @Provides
  def providesChallengeRepository(@Inject
                                  ssdb: SSDB): ChallengeRepository = {
    ChallengeRepository(ssdb)
  }


  @Singleton
  @Provides
  def providesUserChallengeRepository(@Inject ssdb: SSDB): ChallengeHistoryRepository = {
    ChallengeHistoryRepository(ssdb)
  }

  @Singleton
  @Provides
  def providesChallengeWatchlist(@Inject ssdb: SSDB): ChallengeWatchList = {
    ChallengeWatchListImpl(ssdb)
  }

  @Singleton
  @Provides
  def providesChallengeWatchlist(@Inject watchList: ChallengeWatchList,
                                 repository: ChallengeRepository): ChallengeWatcher = {
    ExpireChallengeWatcher(watchList, repository)
  }

  @Singleton
  @Provides
  def providesChallengeTemplateService(@Inject
                               repository: ChallengeRepository,
                               courseService: CourseService,
                               deckService: DeckService,
                               cardService: CardService,
                               idGenService: IdGenService): ChallengeTemplateService = {

    ChallengeTemplateServiceImpl(repository,
      courseService,
      deckService,
      cardService,
      idGenService)
  }

  @Singleton
  @Provides
  def providesChallengeService(@Inject
                               repository: ChallengeRepository,
                               userChallengeRepository: ChallengeHistoryRepository,
                               watcher: ChallengeWatcher,
                               leaderBoardService: LeaderBoardService,
                               courseService: CourseService,
                               deckService: DeckService,
                               cardService: CardService,
                               idGenService: IdGenService): ChallengeService = {

    ChallengeServiceImpl(repository,
      userChallengeRepository,
      watcher,
      leaderBoardService,
      courseService,
      deckService,
      cardService,
      idGenService)
  }


  @Provides
  @Singleton
  @Named("challenge_processor")
  def providesChallengeProcessor(@Inject botConfig: BotConfig,
                                 challengeService: ChallengeService,
                                 leaderBoardService: LeaderBoardService,
                                 srsService: SRSService,
                                 cardService: CardService): TestProcessor = {

    test.TestChallengeProcessor(botConfig,
      challengeService,
      leaderBoardService,
      srsService,
      cardService)
  }

  @Provides
  @Singleton
  @Named("challenge_create_handler")
  def providesChallengeCreateHandler(@Inject botConfig: BotConfig,
                                   nlpService: NLPService,
                                     analytics: Analytics,
                                     templateService: ChallengeTemplateService,
                                   challengeService: ChallengeService,
                                   leaderBoardService: LeaderBoardService): ActionHandler = {
    ChallengeCreateHandler(nlpService,
      botConfig,
      analytics,
      templateService,
      challengeService,
      leaderBoardService
    )
  }

  @Provides
  @Singleton
  @Named("challenge_join_handler")
  def providesChallengeJoinHandler(@Inject botConfig: BotConfig,
                                   nlpService: NLPService,
                                   analytics: Analytics,
                                   templateService: ChallengeTemplateService,
                                   challengeService: ChallengeService,
                                   leaderBoardService: LeaderBoardService): ActionHandler = {
    ChallengeJoinHandler(nlpService,
      botConfig,
      analytics,
      templateService,
      challengeService,
      leaderBoardService
    )
  }

  @Provides
  @Singleton
  @Named("challenge_play_handler")
  def providesChallengePlayHandler(@Inject botConfig: BotConfig,
                                   nlpService: NLPService,
                                   analytics: Analytics,
                                   @Named("challenge_processor") testProcessor: TestProcessor): ActionHandler = {

    val fibQuestionHandler = ChallengeFIBHandler(
      botConfig,
      testProcessor
    )

    val mcQuestionHandler = ChallengeMCHandler(botConfig,
      testProcessor
    )

    ChallengePlayHandler(nlpService,
      botConfig,
      analytics,
      testProcessor,
      fibQuestionHandler,
      mcQuestionHandler)
  }

  @Provides
  @Singleton
  @Named("challenge_known_question_handler")
  def providesChallengeKnownQuestionHandler(@Inject botConfig: BotConfig,
                                       nlpService: NLPService,
                                            analytics: Analytics,
                                       @Named("challenge_processor") testProcessor: TestProcessor
                                      ): ActionHandler = {
    challenge.ChallengeRememberHandler(nlpService,
      botConfig,
      analytics,
      testProcessor
    )
  }

  @Provides
  @Singleton
  @Named("challenge_dont_known_question_handler")
  def providesChallengeDontKnownQuestionHandler(@Inject botConfig: BotConfig,
                                           nlpService: NLPService,
                                                analytics: Analytics,
                                           @Named("challenge_processor") testProcessor: TestProcessor): ActionHandler = {

    challenge.ChallengeNotRememberHandler(nlpService,
      botConfig,
      analytics,
      testProcessor
    )
  }

  @Provides
  @Singleton
  @Named("challenge_continue_handler")
  def providesChallengeContinueHandler(@Inject botConfig: BotConfig,
                                                nlpService: NLPService,
                                       analytics: Analytics,
                                                @Named("challenge_processor") testProcessor: TestProcessor): ActionHandler = {

    challenge.ChallengeContinueHandler(nlpService,
      botConfig,
      analytics,
      testProcessor
    )
  }


  @Provides
  @Singleton
  @Named("challenge_submit_handler")
  def providesChallengeSubmitHandler(@Inject botConfig: BotConfig,
                                     nlpService: NLPService,
                                     analytics: Analytics,
                                     @Named("challenge_processor") testProcessor: TestProcessor): ActionHandler = {
    challenge.ChallengeSubmitHandler(nlpService,
      botConfig,
      analytics,
      testProcessor
    )
  }




}

