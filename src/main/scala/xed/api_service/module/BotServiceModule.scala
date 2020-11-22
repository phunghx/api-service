package xed.api_service.module

import java.io.File

import akka.actor.ActorRef
import com.google.inject.name.Named
import com.google.inject.{Inject, Provides, Singleton}
import com.twitter.finagle.thrift
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.repository.ESConfig
import xed.api_service.service.course.{CourseService, JourneyService}
import xed.api_service.service.{MessageService => _, _}
import xed.api_service.util._
import xed.chatbot._
import xed.chatbot.domain.BotConfig
import xed.chatbot.repository.{CourseLearningRepository, MessageRepository, SSDBMessageRepository, UserCourseLearningRepository}
import xed.chatbot.service.{LearnService, _}
import xed.dictionary.service.TDictionaryService
import xed.userprofile.UserProfileService

import scala.io.Source

/**
  * Created by phg on 2019-09-29.
  **/
object BotServiceModule extends BotServiceModule {

}

class BotServiceModule extends XedModule {

  override def configure(): Unit = {
    super.configure()

    bind[String].annotatedWithName("dialogflow_project_id").toInstance(ZConfig.getString("diaflow.project-id"))
    bind[String].annotatedWithName("dialogflow_credential_file").toInstance(ZConfig.getString("diaflow.credential-file"))
  }


  @Singleton
  @Provides
  def providesDictionaryService: DictionaryService = {
    import com.twitter.conversions.time._
    import com.twitter.finagle.Thrift
    import com.twitter.finagle.service.{Backoff, RetryBudget}
    import com.twitter.util.Duration

    val host = ZConfig.getString("dictionary.thrift.host")
    val port = ZConfig.getInt("dictionary.thrift.port")
    val timeoutInSecs = ZConfig.getInt("dictionary.thrift.timeout_in_secs", 5)
    val label = "dictionary-from-chatbot"

    val client = Thrift.client
      .withRequestTimeout(Duration.fromSeconds(timeoutInSecs))
      .withSessionPool
      .minSize(1)
      .withSessionPool
      .maxSize(10)
      .withRetryBudget(RetryBudget())
      .withRetryBackoff(Backoff.exponentialJittered(5.seconds, 32.seconds))
      .withClientId(thrift.ClientId(label))
      .build[TDictionaryService.MethodPerEndpoint](s"$host:$port", label)

    DictionaryServiceImpl(client)
  }


  @Provides
  @Singleton
  def providesKikiBotConfig(): BotConfig = {
    Implicits.usingSource(Source.fromFile(new File("./conf/chatbot_config.json"), "utf-8")) {
      source => {
        val content = source.getLines().mkString("\n")
        JsonUtils.fromJson[BotConfig](content)
      }
    }
  }

  @Provides
  @Singleton
  @Named("es_kikibot")
  def providesKikiBotESConfig(): ESConfig = readESConfig("es_kikibot")


  @Provides
  @Singleton
  def providesUserLearningRepository(@Inject ssdb: SSDB): CourseLearningRepository = {
    UserCourseLearningRepository(
      ssdb,
      ZConfig.getConf("study_repo"))
  }


  @Provides
  @Singleton
  def providesLearnService(@Inject
                           repository: CourseLearningRepository,
                           srsService: SRSService,
                           courseService: CourseService,
                           journeyService: JourneyService,
                           deckService: DeckService,
                           cardService: CardService,
                           profileService: UserProfileService): LearnService = {
    LearnServiceImpl(
      repository,
      srsService,
      courseService,
      journeyService,
      deckService,
      cardService,
      profileService)
  }




  @Singleton
  @Provides
  def providesIdGenService(@Inject ssdb: SSDB): IdGenService = {
    IdGenServiceImpl(ssdb)
  }

  @Singleton
  @Provides
  def providesMessageRepository(@Inject  ssdb: SSDB): MessageRepository = {
    SSDBMessageRepository(
      ssdb,
      ZConfig.getString("bot.inbox.key")
    )
  }

  @Singleton
  @Provides
  def providesMessageService(@Inject repository: MessageRepository,
                             ssdb: SSDB,
                             idGenService: IdGenService): MessageService = {
    KiKiBotMessageService(
      repository,
      idGenService
    )
  }

  @Provides
  @Singleton
  def providesGatewayService(@Inject idGenService: IdGenService,
                             messageService: MessageService,
                             @Named("kiki-message-actor") messageActor: ActorRef): GatewayService = {
    GatewayServiceImpl(
      idGenService,
      messageService,
      messageActor
    )
  }

  @Provides
  @Singleton
  def providesNLPService(@Inject
                         @Named("dialogflow_project_id") projectId: String,
                         @Named("dialogflow_credential_file") credentialFile: String): NLPService = {
    DialogFlowNLPService(projectId, credentialFile)
  }


  @Provides
  @Singleton
  def providesActionResolvers(@Inject nlpService: NLPService): Seq[ActionResolver] = {
    Seq(
      SystemActionResolver(),
      NLPActionResolver(nlpService)
    )
  }

  @Provides
  @Singleton
  def providesActionResolveEngine(@Inject resolvers: Seq[ActionResolver]): ActionResolveEngine = {
    ActionResolveEngineImpl(resolvers)
  }

}
