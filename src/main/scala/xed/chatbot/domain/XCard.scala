package xed.chatbot.domain

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import xed.api_service.domain.design.{BotActionDeserializer, Component}


@SerialVersionUID(20200602L)
case class XCard(`type`: String = "xcard",
                 version: Int,
                 body: Seq[Component],
                 actions: Seq[UserAction] = Seq.empty,
                 fallbackText: Option[String] = None,
                 background: Option[String] = None,
                 speak: Option[String] = None,
                 effectId: Option[String] = None,
                 suggestions: Seq[UserAction] = Seq.empty) extends Serializable

object UserAction {
  val PostBack = "postback"
  val ShareChallenge = "share_challenge"
}

@JsonDeserialize(using = classOf[BotActionDeserializer])
trait UserAction extends Serializable {
  val `type`: String
  val messageType : String
  val title: String
  val value: String

}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class PostBackUAction(title: String,
                           value: String,
                           messageType: String = MessageType.TEXT) extends UserAction {
  override val `type` = UserAction.PostBack
}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class ShareChallengeUAction(title: String,
                           value: String) extends UserAction {
  override val `type` = UserAction.ShareChallenge
  override val messageType: String = MessageType.TEXT
}




