package xed.chatbot.domain.challenge

import com.fasterxml.jackson.annotation.JsonIgnore
import education.x.commons.Serializer
import org.apache.commons.lang.SerializationUtils
import xed.api_service.domain.exception.InternalError
import xed.api_service.util.Utils
import xed.userprofile.domain.ShortUserProfile

import scala.collection.mutable

/**
 * @author andy
 * @since 2/26/20
 **/
object ChallengeType {
  val DEFAULT  = "default"
  val STREAKING  = "streaking"
}



object Challenge {
  implicit object ChallengeSerializer extends Serializer[Challenge] {
    override def fromByte(bytes: Array[Byte]): Challenge = {
      SerializationUtils.deserialize(bytes).asInstanceOf[Challenge]
    }

    override def toByte(value: Challenge): Array[Byte] = {
      SerializationUtils.serialize(value.asInstanceOf[Serializable])
    }
  }

}

@SerialVersionUID(26032020L)
case class Challenge(challengeId: Int,
                     challengeType: Option[String],
                     name: Option[String],
                     description: Option[String],
                     creator: Option[String],
                     canExpired: Boolean,
                     questionListId: String,
                     status: Option[Int],
                     isFinished: Option[Boolean],
                     createdTime: Option[Long],
                     dueTime: Option[Long],
                     questionCount: Option[Int] = Some(0),
                     creatorDetail: Option[ShortUserProfile] = None) extends Serializable {

  @JsonIgnore
  def getDuration() = dueTime.get - createdTime.get

  @JsonIgnore
  def getReadableDueDate(): String = {
    Utils.timeToString(
      dueTime.getOrElse(0L),
      "E, dd MMM yyyy HH:mm z"
    )
  }

  @JsonIgnore
  def getReadableChallengeType(questionCount: Int): String = {
    challengeType.get match {
      case ChallengeType.STREAKING =>s"A Streaking Game with ${questionCount} questions."
      case _ => s"A Game with ${questionCount} questions."
    }
  }

  @JsonIgnore
  def isAlreadyFinished(): Boolean = {
    canExpired match {
      case true => isFinished.getOrElse(false) || dueTime.getOrElse(0L) <= System.currentTimeMillis()
      case _ => false
    }
  }
}

case class ChallengeAnswerInfo(challengeId: Int,
                               username: String,
                               correctIds: mutable.Set[String],
                               incorrectIds: mutable.Set[String],
                               duration: Long,
                               lastAnswerTime: Long)