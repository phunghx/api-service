package xed.chatbot.domain.challenge

import education.x.commons.Serializer
import org.apache.commons.lang.SerializationUtils

object ChallengeTemplate {
  implicit object ChallengeTemplateSerializer extends Serializer[ChallengeTemplate] {
    override def fromByte(bytes: Array[Byte]): ChallengeTemplate = {
      SerializationUtils.deserialize(bytes).asInstanceOf[ChallengeTemplate]
    }

    override def toByte(value: ChallengeTemplate): Array[Byte] = {
      SerializationUtils.serialize(value.asInstanceOf[Serializable])
    }
  }

}


case class ChallengeTemplate(templateId: String,
                             challengeType: String,
                             name: String,
                             description: Option[String],
                             canExpired: Boolean,
                             duration: Option[Long],
                             questionListId: String)
