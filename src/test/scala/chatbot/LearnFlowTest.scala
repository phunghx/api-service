package chatbot

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import xed.api_service.Server
import xed.api_service.module._
import xed.chatbot.service.BotService
import xed.chatbot.service.simulator.ChatFlowSimulator
import xed.userprofile.AuthenService


class LearnFlowTest extends IntegrationTest {


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


  test("Learn flow") {

    chatFlowSimulator.sendText("I want to learn vocabulary")
    chatFlowSimulator.sendText("abc")
    chatFlowSimulator.sendText("Learn this word")

  }


}
