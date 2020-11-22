package xed.chatbot.domain

import education.x.commons.Serializer
import org.apache.commons.lang.SerializationUtils
import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.{JsonUtils, ZConfig}
import xed.chatbot.domain.challenge.Challenge
import xed.userprofile.domain.ShortUserProfile

object MessageStatus {
  val CREATED = 0
  val RECEIVED = 1
  val SEEN = 2
  val DELETED = 3
}

object MessageType {
  val TEXT = "text"
  val REPLY_MC ="reply_mc"
  val REPLY_FIB ="reply_fib"
}

object ChatMessage {


  val KIKIBOT = ZConfig.getString("bot.username")

  def create(id: Int = -1,
             card: Option[XCard],
             currentAction: Option[IntentActionInfo] = None,
             sender: Option[String],
             recipient: Option[String],
             postDelayMillis: Option[Int] = Some(800),
             senderDetail: Option[ShortUserProfile] = None,
             recipientDetail: Option[ShortUserProfile] = None) = {

    ChatMessage(
      id = id,
      currentAction = currentAction,
      sender = sender,
      recipient = recipient,
      card = card,
      messageStatus = Some(MessageStatus.CREATED),
      postDelayMillis = postDelayMillis,
      updatedTime = Some(System.currentTimeMillis()),
      sentTime = Some(System.currentTimeMillis()),
      senderDetail = senderDetail,
      recipientDetail = recipientDetail
    )
  }

  implicit object ChatMessageSerializer extends Serializer[ChatMessage] {
    override def fromByte(bytes: Array[Byte]): ChatMessage = {
      SerializationUtils.deserialize(bytes).asInstanceOf[ChatMessage]
    }

    override def toByte(value: ChatMessage): Array[Byte] = {
      SerializationUtils.serialize(value.asInstanceOf[Serializable])
    }
  }
}

@SerialVersionUID(20200602L)
case class ChatMessage(id: Int,
                       messageType: Option[String] = Some(MessageType.TEXT),
                       currentAction: Option[IntentActionInfo] = None,
                       sender: Option[String],
                       recipient: Option[String],
                       card: Option[XCard],
                       languageCode: Option[String] = None,
                       text : Option[String] = None,
                       messageStatus: Option[Int] = Some(MessageStatus.CREATED),
                       postDelayMillis: Option[Int] = Some(0),
                       updatedTime: Option[Long],
                       sentTime: Option[Long],
                       var senderDetail: Option[ShortUserProfile] = None,
                       var recipientDetail: Option[ShortUserProfile] = None) extends Serializable


