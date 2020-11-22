package chatbot

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import xed.api_service.Server
import xed.api_service.module._
import xed.chatbot.service.BotService
import xed.chatbot.service.simulator.ChatFlowSimulator
import xed.userprofile.AuthenService



class SearchDictFlowTest extends IntegrationTest {


  override protected def injector: Injector = Injector(Guice.createInjector(
    Server.overrideModule(XedApiModule,
      XedApiModuleTestImpl,
      PublicPathConfigModule,
      BotServiceModule,
      BotProcessorModule,
      ChallengeModule,
      HandlerModule)
  ))

  val chatFlowSimulator = ChatFlowSimulator(
    isDisplayConversation = true,
    botService = injector.instance[BotService],
    authenService = injector.instance[AuthenService]
  )
  /**
   * - Me: What's the meaning of dog
   * - Bot: Show dictionary card for DOG
   * - Me: verb (change its part of speech to VERB)
   * - Bot: Show dictionary card for dog in VERB
   * - Me: red
   * - Bot: Show dictionary card for RED
   * - Me: Exit
   * - Bot: (Say goodbye)
   */
  test("Search dictionary conversation") {

    chatFlowSimulator.sendText("What's the meaning of dog")
    chatFlowSimulator.sendText("verb")
    chatFlowSimulator.sendText("red")
    chatFlowSimulator.sendText("Exit")

  }


}
