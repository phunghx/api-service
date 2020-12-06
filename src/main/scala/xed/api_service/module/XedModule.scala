package xed.api_service.module

import java.net.InetAddress
import java.util.TimeZone

import akka.actor.{ActorRef, ActorSystem, Props}
import com.google.inject.{Inject, Provides, Singleton}
import com.twitter.finagle.thrift
import com.twitter.inject.TwitterModule
import com.typesafe.config.{Config, ConfigRenderOptions}
import javax.inject.Named
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.nutz.ssdb4j.SSDBs
import org.nutz.ssdb4j.spi.SSDB
import redis.clients.jedis.JedisPool
import xed.api_service.repository.card.{CardRepository, SSDBCardRepository}
import xed.api_service.repository.{SRSRepository, _}
import xed.api_service.service._
import xed.api_service.service.bq.{HttpLogService, LogService}
import xed.api_service.service.sendgrid.{SendGridService, SendGridServiceImpl}
import xed.api_service.service.statistic._
import xed.api_service.util.ZConfig
import xed.api_service.util.ZConfig.ZConfigLike
import xed.caas.service.TCaasService
import xed.notification.{HttpNotificationService, NotificationService}
import xed.userprofile._
import xed.userprofile.service.TUserProfileService


object XedApiModuleTestImpl extends TwitterModule {
  @Singleton
  @Provides
  def providesAuthenService: AuthenService = AuthenServiceTestImpl()

  @Singleton
  @Provides
  def providesUserProfileService: UserProfileService = UserProfileServiceTestImpl()


}

class XedModule extends TwitterModule {
  def readESConfig(key: String): ESConfig = {
    import scala.collection.JavaConversions._

    val cfg = ZConfig.loadFile("conf/es.mapping.conf")
    val settings = cfg.getConf(s"$key.settings")
      .root()
      .render(ConfigRenderOptions.concise())
    val mappings = cfg.getConf(s"$key.mappings")
      .root()
      .entrySet()
      .map(t => t.getKey -> t.getValue.render(ConfigRenderOptions.concise()))
      .toMap
    val indexName = cfg.getString(s"$key.index_name")

    ESConfig(indexName, settings, mappings)
  }
}


object XedApiModule extends XedModule {

  override def configure(): Unit = {
    super.configure()

    bind[Config].annotatedWithName("remote-logger-config").toInstance(ZConfig.getConf("remote-logger"))

    bind[Config].annotatedWithName("user-profile-config").toInstance(ZConfig.getConf("user-profile-repo"))
    bind[Config].annotatedWithName("notification-config").toInstance(ZConfig.getConf("notification"))
    bind[Config].annotatedWithName("sendgrid-config").toInstance(ZConfig.getConf("sendgrid"))
    bind[Config].annotatedWithName("weekly-report-config").toInstance(ZConfig.getConf("report.weekly"))

    bind[TimeZone].toInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))

    bind[CardService].to[CardServiceImpl].asEagerSingleton()

    bind[CountService].to[CountServiceImpl].asEagerSingleton()
    bind[HistoryService].to[HistoryServiceImpl].asEagerSingleton()
    bind[StatisticService].to[StatisticServiceImpl].asEagerSingleton()
    bind[AlertService].to[SimpleAlertService].asEagerSingleton()

    bind[LogService].to[HttpLogService].asEagerSingleton()

    bind[Analytics].to[AnalyticsImpl].asEagerSingleton()

    bind[NotificationService].to[HttpNotificationService].asEagerSingleton()
    bind[SendGridService].to[SendGridServiceImpl].asEagerSingleton()
  }





  @Singleton
  @Provides
  def providesDeckService(@Inject deckRepository: DeckRepository,
                          cardRepository: CardRepository,
                          analytics: Analytics,
                          @Named("event-publisher-router") eventPublisher: ActorRef): DeckService = {

    DeckServiceImpl(deckRepository,
      cardRepository,
      analytics,
      eventPublisher)
  }

  @Singleton
  @Provides
  def providesRedisPool(): JedisPool = {
    import redis.clients.jedis.JedisPoolConfig

    val host = ZConfig.getString("redis.host")
    val port = ZConfig.getInt("redis.port")
    val authPass = ZConfig.getString("redis.auth_pass", null)
    val timeout = ZConfig.getInt("redis.timeout", 15)
    val maxTimeoutInMillis = ZConfig.getInt("redis.max_timeout_millis", 60000)

    val poolConfig = new JedisPoolConfig
    poolConfig.setMaxWaitMillis(maxTimeoutInMillis)
    poolConfig.setMaxTotal(16)
    poolConfig.setTestWhileIdle(true)
    new JedisPool(
      poolConfig,
      host,
      port,
      timeout,
      authPass
    )
  }

  @Provides
  @Singleton
  def providesActorSystem(): ActorSystem = {
    ActorSystem(ZConfig.getString("slack.system_name", "api-service"))
  }


  @Provides
  @Singleton
  @Named("event-publisher-router")
  def providesDeckEventPublisher(@Inject redisPool: JedisPool,
                                 deckRepository: DeckRepository,
                                 cardRepository: CardRepository,
                                 srsRepository: SRSRepository,
                                 countService: CountService,
                                 system: ActorSystem): ActorRef = {
    system.actorOf(Props(
      classOf[EventPublisher],
      redisPool,
      deckRepository,
      cardRepository,
      srsRepository,
      countService
    ), "publisher-router")
  }

  @Singleton
  @Provides
  def providesAuthService: AuthenService = {
    import com.twitter.conversions.time._
    import com.twitter.finagle.Thrift
    import com.twitter.finagle.service.{Backoff, RetryBudget}
    import com.twitter.util.Duration

    val host = ZConfig.getString("auth.thrift.host")
    val port = ZConfig.getInt("auth.thrift.port")
    val timeoutInSecs = ZConfig.getInt("auth.thrift.timeout_in_secs", 5)
    val label = "auth-caas-service-from-api-service"

    val client = Thrift.client
      .withRequestTimeout(Duration.fromSeconds(timeoutInSecs))
      .withSessionPool.minSize(1)
      .withSessionPool.maxSize(10)
      .withRetryBudget(RetryBudget())
      .withRetryBackoff(Backoff.exponentialJittered(5.seconds, 32.seconds))
      .withClientId(thrift.ClientId(label))
      .build[TCaasService.MethodPerEndpoint](s"$host:$port", label)

    new AuthenServiceImpl(client)
  }

  @Singleton
  @Provides
  def providesAuthHolder(@Inject authService: AuthenService): SessionHolder = {
    SessionHolderImpl(authService)
  }

  @Singleton
  @Provides
  def providesUserProfileService: UserProfileService = {
    import com.twitter.conversions.time._
    import com.twitter.finagle.Thrift
    import com.twitter.finagle.service.{Backoff, RetryBudget}
    import com.twitter.util.Duration

    val host = ZConfig.getString("user_profile.thrift.host")
    val port = ZConfig.getInt("user_profile.thrift.port")
    val timeoutInSecs = ZConfig.getInt("user_profile.thrift.timeout_in_secs", 5)
    val label = "user-profile-from-api-service"

    val client = Thrift.client
      .withRequestTimeout(Duration.fromSeconds(timeoutInSecs))
      .withSessionPool.minSize(1)
      .withSessionPool.maxSize(10)
      .withRetryBudget(RetryBudget())
      .withRetryBackoff(Backoff.exponentialJittered(5.seconds, 32.seconds))
      .withClientId(thrift.ClientId(label))
      .build[TUserProfileService.MethodPerEndpoint](s"$host:$port", label)

    UserProfileServiceImpl(client)
  }


  @Singleton
  @Provides
  def providesSSDB(): SSDB = {
    SSDBs.pool(
      ZConfig.getString("ssdb.config.host"),
      ZConfig.getInt("ssdb.config.port"),
      ZConfig.getInt("ssdb.config.timeout_in_ms"), null)
  }

  @Provides
  @Singleton
  def providesESClient(): TransportClient = {
    val clusterName = ZConfig.getString("es_client.cluster_name")
    val client = new PreBuiltTransportClient(Settings.builder()
      .put("cluster.name", clusterName)
      .put("client.transport.sniff", "false")
      .build())

    ZConfig.getStringList("es_client.servers").map(s => {
      val hostPort = s.split(":")
      (hostPort(0), hostPort(1).toInt)
    }).foreach(hostPort => {
      info(s"Add ${hostPort._1}:${hostPort._2} to transport address")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostPort._1), hostPort._2))
    })
    client
  }


  @Provides
  @Singleton
  @Named("es_deck")
  def providesDeckESConfig(): ESConfig = readESConfig("es_deck")

  @Provides
  @Singleton
  @Named("es_srs")
  def providesESSRSConfig(): ESConfig = readESConfig("es_srs")


  @Singleton
  @Provides
  def providesDeckRepository(@Inject
  client: TransportClient,
    @Named("es_deck") conf: ESConfig): DeckRepository = {
    val esType = ZConfig.getString("es_client.deck_type")
    DeckRepository(client, conf, esType)
  }


  @Singleton
  @Provides
  def providesSSDBCardRepository(@Inject ssdb: SSDB): CardRepository = {
    val cardHashMapName = ZConfig.getString("es_client.ssdb_card_hashmap_name")
    SSDBCardRepository(ssdb,
      cardHashMapName
    )
  }

  @Singleton
  @Provides
  def providesSRSRepository(@Inject
  client: TransportClient,
    @Named("es_srs") conf: ESConfig): SRSRepository = {
    val esType = ZConfig.getString("es_client.srs_type")
    SRSRepository(client, conf, esType)
  }


  @Singleton
  @Provides
  def providesSRSService(@Inject
    srsRepository: SRSRepository,
    deckService: DeckService,
    cardService: CardService,
    historyService: HistoryService,
    profileService: UserProfileService,
                         analytics: Analytics,
    @Named("event-publisher-router") eventPublisher: ActorRef): SRSService = {
    SRSServiceImpl(
      repository = srsRepository,
      cardService = cardService,
      deckService = deckService,
      historyService = historyService,
      profileService = profileService,
      analytics,
      eventPublisher
    )
  }


  @Singleton
  @Provides
  def providesReviewHistoryRepository(@Inject
  client: TransportClient,
    @Named("es_srs") conf: ESConfig): ReviewHistoryRepository = {
    val esType = ZConfig.getString("es_client.review_type")
    ReviewHistoryRepository(client, conf, esType)
  }

  @Singleton
  @Provides
  def providesGenerateCardService(): GenerateCardService = {
    val host = ZConfig.getString("hosts.api")
    GenerateCardServiceImpl(host)
  }


}
