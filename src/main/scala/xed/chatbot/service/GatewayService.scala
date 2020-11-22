package xed.chatbot.service

import akka.actor.{Actor, ActorRef}
import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.api_service.domain.exception.InternalError
import xed.api_service.util.Implicits._
import xed.chatbot.domain.{ChatMessage, ChatRequest}
import xed.chatbot.service.BotMessageEvents.{ChatEvent, ConversationEndEvent, ConversationStartEvent}
import xed.profiler.Profiler
import xed.userprofile.SignedInUser


trait GatewayService {

  def conversationStart(sender: SignedInUser) : Future[Boolean]

  def conversationEnd(sender: SignedInUser) : Future[Boolean]

  def chat(sender: SignedInUser, request: ChatRequest): Future[ChatMessage]
}

case class GatewayServiceImpl(idGenService: IdGenService,
                              messageService: MessageService,
                              messageActor: ActorRef) extends GatewayService {


  override def conversationStart(sender: SignedInUser): Future[Boolean] = {
    idGenService.genMessageId().map(messageId => {
      val msg = ChatMessage.create(
        id = messageId,
        None,
        sender = Some(sender.username),
        currentAction = None,
        recipient = None
      )
      messageActor ! ConversationStartEvent(sender, msg)

      true
    })
  }

  override def conversationEnd(sender: SignedInUser): Future[Boolean] = {
    messageActor ! ConversationEndEvent(sender)
    Future.True
  }

  override def chat(sender: SignedInUser, request: ChatRequest): Future[ChatMessage] =  {
    idGenService.genMessageId().map(messageId => {

      val message = request.buildMessage(sender, messageId)
      messageActor ! ChatEvent( sender, message)
      message

    })

  }


}

object BotMessageEvents {

  case class ConversationStartEvent(sender: SignedInUser,  message: ChatMessage)

  case class ChatEvent(sender: SignedInUser, message: ChatMessage)

  case class ConversationEndEvent(sender: SignedInUser)

}


case class MessageEventProcessor(botService: BotService,
                                 messageService: MessageService) extends Actor with Logging {

  private lazy val clazz = getClass.getSimpleName


  override def receive: Receive = {
    case event: ChatEvent => chat(event.sender, event.message)
    case event: ConversationStartEvent => conversationStart(event.sender, event.message)
    case event: ConversationEndEvent => conversationEnd(event.sender)
    case x => logger.info(s"Received an unknown message: $x")
  }

  private def chat(sender: SignedInUser, message: ChatMessage): Unit = Profiler(s"${clazz}.handleChat") {
    val fn = for {
      messages <- botService.processMessage(sender, message)
      sentMessages <- messageService.send(sender, messages)
    } yield sentMessages.nonEmpty match {
      case true => sentMessages
      case _ =>
        throw InternalError(Some(s"Can't send ${messages.size} messages to user: ${sender.username}"))
    }

    fn.sync()
  }

  private def conversationStart(sender: SignedInUser,  message: ChatMessage): Unit = Profiler(s"${clazz}.conversationStart") {
    val fn = for {
      messages <- botService.conversationStart(sender, message)
      sentMessages <- messageService.send(sender, messages)
    } yield {
      sentMessages
    }
    fn.sync()
  }

  private def conversationEnd(sender: SignedInUser): Unit = Profiler(s"${clazz}.conversationEnd") {
    messageService.clearMessages(sender).sync()
  }
}


