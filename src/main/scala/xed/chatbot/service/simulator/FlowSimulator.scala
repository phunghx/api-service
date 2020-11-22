package xed.chatbot.service.simulator

import com.twitter.util.Future
import xed.api_service.util.Implicits.FutureEnhance
import xed.chatbot.domain.{ChatMessage, IntentActionInfo, MessageType}
import xed.chatbot.service.BotService
import xed.chatbot.service.simulator.FlowSimulator.ChatFlowResponse
import xed.userprofile.{AuthenService, SignedInUser}
/**
 * @author andy
 * @since 2/16/20
 **/
object FlowSimulator {
  case class ChatFlowResponse(myMessage: ChatMessage, botMessages: Seq[ChatMessage])
}

abstract class FlowSimulator {

  val isDisplayConversation: Boolean
  val botService: BotService
  val authenService: AuthenService


  var currentAction: Option[IntentActionInfo] = None
  val ssid = "e3a7c48a-ec78-4ced-bbf5-e4d8cec3da7d"
  val sender = "up-05d791b2-718a-46e9-aa0e-84dffd547fde"

  final def sendText(text: String, currentActionInfo: Option[IntentActionInfo] = None): ChatFlowResponse = {
    send(MessageType.TEXT, text, currentActionInfo)
  }

  final def sendAnswerMultiChoice(options: Seq[Int], currentActionInfo: Option[IntentActionInfo] = None): ChatFlowResponse = {
    val text = options.map(x => x + 1).mkString(", ")
    send(MessageType.REPLY_MC, text, currentActionInfo)
  }

  final def sendAnswerFIB(text: String, currentActionInfo: Option[IntentActionInfo] = None): ChatFlowResponse = {
    send(MessageType.REPLY_FIB, text, currentActionInfo)
  }

  private final def send(messageType: String,
                         text: String,
                         currentActionInfo: Option[IntentActionInfo]): ChatFlowResponse = {
    val message = ChatMessage(
      id = 0,
      messageType = Some(messageType),
      sender = Some(sender),
      recipient = Some("kiki"),
      card = None,
      text = Some(text),
      currentAction = if(currentActionInfo.isDefined) currentActionInfo else currentAction,
      updatedTime = Some(System.currentTimeMillis()),
      sentTime = Some(System.currentTimeMillis())
    )

    val fn= for {
      sender <- getTestSignedInUser()
      repliedMessage <- botService.processMessage(sender, message)
      r =  handleRepliedMessage(message, repliedMessage)
    } yield r

    fn.sync()

  }

  private def getTestSignedInUser(): Future[SignedInUser] = {
    authenService.getUserWithSessionId(ssid).map(_.get).map(x => {
      val roleIds = Option(x.roles.map(_.id).toSet).getOrElse(Set.empty[Int])
      val permissions = Set.empty[String]
      SignedInUser(session = ssid,
        username = x.username,
        userProfile = None,
        roles = roleIds,
        permissions = permissions,
        permissionsWithRole = Map(0 -> permissions))
    })

  }


  private final def handleRepliedMessage(myMessage: ChatMessage,
                                         botMessages: Seq[ChatMessage]): ChatFlowResponse = {
    val lastBotMesssge = botMessages.lastOption

    lastBotMesssge match {
      case Some(message) =>
        currentAction = message.currentAction
      case _ =>
        currentAction = null
    }

    val r =   ChatFlowResponse(myMessage, botMessages)

    if (isDisplayConversation) {
      displayConversation(r)
    }

  r


  }

  private def displayConversation(chatFlowResponse: ChatFlowResponse): Unit = {
    println("===============================================================================╗")
    displayMyMessage(chatFlowResponse.myMessage)
    println("-----------------------------------")
    chatFlowResponse.botMessages.foreach(msg => {
      displayBotMessage(msg)
      println("-----------------------------------")
    })
    println("===============================================================================╝")
  }


  def displayMyMessage(message: ChatMessage)

  def displayBotMessage(message: ChatMessage)

}
