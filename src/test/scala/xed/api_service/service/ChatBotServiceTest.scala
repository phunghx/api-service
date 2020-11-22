package xed.api_service.service

import com.google.inject.{Guice, Inject, Provides}
import com.twitter.inject.{Injector, IntegrationTest, TwitterModule}
import xed.chatbot.service.BotService

/**
 * Created by phg on 2019-09-17.
 **/
class ChatBotServiceTest extends IntegrationTest {

  override protected def injector: Injector = Injector(Guice.createInjector(NLPModule))

  private def chatBotService = injector.instance[BotService]

  private def testUser = "test-user"

}

object NLPModule extends TwitterModule {

  @Provides
  def providesChatBotService(@Inject nlpService: NLPService): BotService = null
}