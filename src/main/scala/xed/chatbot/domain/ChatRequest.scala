package xed.chatbot.domain

import xed.api_service.domain.design.v100.Text
import xed.userprofile.SignedInUser

case class ChatBodyRequest(messageType: Option[String] = Some(MessageType.TEXT),
                           message: String,
                           currentAction: Option[IntentActionInfo] = None)



case class ChatRequest(sender: SignedInUser,
                       messageType: Option[String] = Some(MessageType.TEXT),
                       message: String,
                       currentAction: Option[IntentActionInfo] = None,
                       languageCode: Option[String] = None,
                       createdTime: Option[Long] = None) {

  def buildMessage(sender: SignedInUser, messageId: Int) = {
    val card = XCard(
      version = 1,
      body = Seq(Text(message)),
      actions = Seq.empty,
      fallbackText = None,
      background = None,
      speak = None,
      suggestions = Seq.empty
    )

    ChatMessage(
      id = messageId,
      messageType = messageType,
      currentAction = currentAction,
      sender = Some(sender.username),
      recipient = Some(ChatMessage.KIKIBOT),
      text = Option(message),
      card = Some(card),
      updatedTime = Some(System.currentTimeMillis()),
      messageStatus = Some(MessageStatus.RECEIVED),
      sentTime = Some(System.currentTimeMillis())
    )
  }
}

case class MarkAlertReadRequest(alertIds: Seq[Int])
case class MarkMessageReadRequest(messageIds: Seq[Int])

case class LearnCardBodyRequest(cardId: String,
                           currentAction: Option[IntentActionInfo] = None)
case class LearnCardRequest(sender: SignedInUser,
                            cardId: String,
                            currentAction: Option[IntentActionInfo] = None,
                            createdTime: Option[Long] = None) {
  def buildMessage(sender: SignedInUser) = {

    val card = XCard(
      `type` = "xcard_learn_card",
      version = 1,
      body = Seq(Text("Learn this card")),
      actions = Seq.empty,
      fallbackText = None,
      background = None,
      speak = None,
      suggestions = Seq.empty
    )

    ChatMessage(
      id = 0,
      currentAction = currentAction,
      sender = Some(sender.username),
      recipient = Some(ChatMessage.KIKIBOT),
      text = Option(cardId),
      card = Some(card),
      updatedTime = Some(System.currentTimeMillis()),
      messageStatus = Some(MessageStatus.RECEIVED),
      sentTime = Some(System.currentTimeMillis())
    )
  }
}


case class ChatResponse(message: ChatMessage,
                        replies: Seq[ChatMessage])
