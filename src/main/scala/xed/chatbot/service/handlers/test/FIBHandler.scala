package xed.chatbot.service.handlers.test

import xed.api_service.domain.design.v100.{FillInBlank, Text}
import xed.api_service.util.Implicits.ImplicitString
import xed.chatbot.domain._

/**
 * @author andy
 * @since 2/17/20
 **/
case class FIBHandler(botConfig: BotConfig, processor: TestProcessor) extends BaseFIBHandler {

}
