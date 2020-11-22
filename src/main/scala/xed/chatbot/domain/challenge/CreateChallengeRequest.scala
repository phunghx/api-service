package xed.chatbot.domain.challenge

import com.twitter.finagle.http.Request
import com.twitter.finatra.validation.{MethodValidation, ValidationResult}
import javax.inject.Inject
import xed.api_service.domain.{Deck, Status}
import xed.api_service.domain.course.CourseInfo

import scala.concurrent.duration.DurationInt

/**
 * @author andy
 * @since 2/29/20
 **/
case class CreateChallengeRequest(name: String,
                                  description: Option[String],
                                  challengeType: String,
                                  canExprired: Boolean,
                                  questionIdList: String,
                                  duration: Option[Long],
                                  status: Option[Int] = Some(Status.PROTECTED.id))

case class CreateChallengeFromDeckRequest(deckId: String,
                                          name: Option[String],
                                          description: Option[String],
                                          challengeType: String,
                                          canExpired: Boolean,
                                          duration: Option[Long],
                                          status: Option[Int] = Some(Status.PROTECTED.id),
                                          addToGlobal: Option[Boolean] = Some(false)) {
  @MethodValidation
  def validateChallengeType(): ValidationResult = {
    ValidationResult.validate(
      Set(ChallengeType.DEFAULT, ChallengeType.STREAKING).contains(challengeType),
      s"Challenge type: $challengeType is not valid."
    )

  }

  @MethodValidation
  def validateChallengeDuration(): ValidationResult = {
    canExpired match {
      case true =>
        val d: Long = duration.getOrElse(0)
        ValidationResult.validate(
          d >= 30.minute.toMillis  && d <= 365.day.toMillis,
          s"duration must be in [30 mins -> 365 days]"
        )
      case _ => ValidationResult.Valid
    }
  }

  def buildChallenge(username: String,
                     challengeId: Int,
                     deck: Deck,
                     questionIdList: String) = {
    Challenge(
      challengeId = challengeId,
      challengeType = Some(challengeType),
      name = name match {
        case Some(x) => Some(x)
        case _ => deck.name
      },
      description = description match {
        case Some(x) => Some(x)
        case _ => deck.description
      },
      canExpired = canExpired,
      creator = Some(username),
      questionListId = questionIdList,
      status = Some(status.getOrElse(Status.PROTECTED.id)),
      isFinished = Some(false),
      createdTime = Some(System.currentTimeMillis()),
      dueTime = duration.map(x => System.currentTimeMillis() + x)
    )

  }

}

case class CreateChallengeFromCourseRequest(courseId: String,
                                            name: Option[String],
                                            description: Option[String],
                                            challengeType: String,
                                            canExpired: Boolean,
                                            duration: Option[Long],
                                            status: Option[Int] = Some(Status.PROTECTED.id),
                                            addToGlobal: Option[Boolean] = Some(false)) {
  @MethodValidation
  def validateChallengeType(): ValidationResult = {
    ValidationResult.validate(
      Set(ChallengeType.DEFAULT, ChallengeType.STREAKING).contains(challengeType),
      s"Challenge type: $challengeType is not valid."
    )

  }

  @MethodValidation
  def validateChallengeDuration(): ValidationResult = {
    canExpired match {
      case true =>
        val d: Long = duration.getOrElse(0)
        ValidationResult.validate(
          d >= 30.minute.toMillis  && d <= 365.day.toMillis,
          s"duration must be in [30 mins -> 365 days]"
        )
      case _ => ValidationResult.Valid
    }
  }

  def toChallenge(username: String,
                  challengeId: Int,
                  courseInfo: CourseInfo,
                  questionIdList: String) = {
    Challenge(
      challengeId = challengeId,
      challengeType = Some(challengeType),
      name = name match {
        case Some(x) => Some(x)
        case _ => courseInfo.name
      },
      description = description match {
        case Some(x) => Some(x)
        case _ => courseInfo.description
      },
      canExpired = canExpired,
      creator = Some(username),
      questionListId = questionIdList,
      status = Some(status.getOrElse(Status.PROTECTED.id)),
      isFinished = Some(false),
      createdTime = Some(System.currentTimeMillis()),
      dueTime = duration.map(x => System.currentTimeMillis() + x)
    )

  }

}


case class UpdateChallengeFromDeckRequest(deckId: String,
                                          name: Option[String],
                                          description: Option[String],
                                          challengeType: String,
                                          canExpired: Boolean,
                                          duration: Option[Long],
                                          addToGlobal: Option[Boolean] = Some(false),
                                         @Inject request: Request) {
  @MethodValidation
  def validateChallengeType(): ValidationResult = {
    ValidationResult.validate(
      Set(ChallengeType.DEFAULT, ChallengeType.STREAKING).contains(challengeType),
      s"Challenge type: $challengeType is not valid."
    )

  }

  @MethodValidation
  def validateChallengeDuration(): ValidationResult = {
    canExpired match {
      case true =>
        val d: Long = duration.getOrElse(0)
        ValidationResult.validate(
          d >= 30.minute.toMillis  && d <= 365.day.toMillis,
          s"duration must be in [30 mins -> 365 days]"
        )
      case _ => ValidationResult.Valid
    }
  }
}


case class CreateChallengeTemplateRequest(name: String,
                                          description: Option[String],
                                          challengeType: String,
                                          canExpired: Boolean,
                                          questionIds: Seq[String],
                                          duration: Option[Long],
                                          status: Option[Int] = Some(Status.PROTECTED.id)) {
  @MethodValidation
  def validateQuestionCount(): ValidationResult = {
    ValidationResult.validate(
      questionIds.nonEmpty,
      "The number of question must greater than 0."
    )

  }
  @MethodValidation
  def validateChallengeType(): ValidationResult = {
    ValidationResult.validate(
      Set(ChallengeType.DEFAULT, ChallengeType.STREAKING).contains(challengeType),
      s"Challenge type: $challengeType is not valid."
    )

  }

  @MethodValidation
  def validateChallengeDuration(): ValidationResult = {
    canExpired match {
      case true =>
        val d: Long = duration.getOrElse(0)
        ValidationResult.validate(
          d >= 30.minute.toMillis  && d <= 365.day.toMillis,
          s"duration must be in [30 mins -> 365 days]"
        )
      case _ => ValidationResult.Valid
    }
  }
}

case class CreateChallengeTemplateFromCourseRequest(
                                                   courseId: String,
                                                     name: Option[String],
                                          description: Option[String],
                                          challengeType: String,
                                          canExpired: Boolean,
                                          duration: Option[Long],
                                                   questionCount: Option[Int],
                                          status: Option[Int] = Some(Status.PROTECTED.id)) {
  @MethodValidation
  def validateChallengeType(): ValidationResult = {
    ValidationResult.validate(
      Set(ChallengeType.DEFAULT, ChallengeType.STREAKING).contains(challengeType),
      s"Challenge type: $challengeType is not valid."
    )

  }

  @MethodValidation
  def validateChallengeDuration(): ValidationResult = {
    canExpired match {
      case true =>
        val d: Long = duration.getOrElse(0)
        ValidationResult.validate(
          d >= 30.minute.toMillis && d <= 365.day.toMillis,
          s"duration must be in [30 mins -> 365 days]"
        )
      case _ => ValidationResult.Valid
    }
  }
}
