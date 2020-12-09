package xed.chatbot.service.simulator
import xed.chatbot.domain.ChatMessage
import xed.chatbot.service.BotService
import xed.userprofile.AuthenService

case class ChatFlowSimulator(isDisplayConversation: Boolean = true,
                             botService: BotService,
                             authenService: AuthenService) extends  FlowSimulator {

  override  def displayMyMessage(message: ChatMessage) = {
    println(s"ME: ${message.text.getOrElse("{EMPTY}")}")
  }

  override  def displayBotMessage(message: ChatMessage) = {
    println(s"BOT: ${message.card}")
  }
}