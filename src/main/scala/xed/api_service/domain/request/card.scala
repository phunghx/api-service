package xed.api_service.domain.request

import java.util.UUID

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.{MethodValidation, NotEmpty, ValidationResult}
import xed.api_service.domain._
import xed.api_service.domain.metric.CountMetric
import xed.api_service.util.{JsonUtils, TimeUtils}
import xed.userprofile.SignedInUser


case class CardRequest(cardVersion: Int, design: Option[CardDesign]) {

  def build(user: SignedInUser, deckId: String) : Card =  {
    Card(
      id = UUID.randomUUID().toString,
      deckId = Some(deckId),
      name = None,
      username = Some(user.username),
      cardVersion = Some(cardVersion),
      design = design.map(JsonUtils.toNode[JsonNode](_)),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}


case class AddMultiCardRequest(@RouteParam deckId: String, cards: Seq[CardRequest]) {

  def build(user: SignedInUser, deckId: String) : Seq[Card]  = {
    cards.map(r => r.build(user,deckId))
  }

}

case class RemoveMultiCardRequest(@RouteParam deckId: String, cardIds: Seq[String]) {
}


case class AddCardRequest(@RouteParam deckId: String,
                          cardVersion: Int,
                          design: Option[CardDesign])  {

  def build(user: SignedInUser) : Card  = CardRequest(cardVersion,design).build(user,deckId)

}

case class AddNewsCardRequest(
  cardVersion: Int,
  design: Option[CardDesign]
) {
  def build(deckId: String, user: SignedInUser) : Card  = CardRequest(cardVersion, design).build(user, deckId)
}


case class GetCardRequest(@NotEmpty cardIds: Seq[String])

case class RemoveFromSRSRequest(@NotEmpty cardIds: Seq[String])


case class EditCardRequest(@RouteParam("card_id") cardId: String,
                           design: Option[CardDesign])





case class AddSRSRequest(@NotEmpty cardIds: Seq[String])

object ReviewRequest {
  val INCORRECT_ANSWER = 0
  val DONT_KNOWN_ANSWER = 1
  val CORRECT_ANSWER = 2
}

case class ReviewRequest(@RouteParam("card_id") cardId: String,
                         answer: Int,
                         duration: Option[Int]) {
  @MethodValidation
  def validation() = {

    val r = ValidationResult.validate( answer>= 0 && answer <=2, "the answer must be 0,1 or 2.")
    r
  }

  def build(success: Boolean,
            user: SignedInUser,
            model: Option[ReviewInfo] = None) = {
    ReviewHistoryInfo(
      id = UUID.randomUUID().toString,
      success = Some(success),
      username = Some(user.username),
      deckId = model.flatMap(_.deckId),
      cardId = model.flatMap(_.cardId),
      memorizingLevel = model.flatMap(_.memorizingLevel),
      answer= Some(answer),
      duration = duration,
      createdTime = Some(System.currentTimeMillis())
    )
  }

  def buildCountMetric(user: SignedInUser) = {
    val (time, _) = TimeUtils.calcBeginOfDayInMillsFrom(System.currentTimeMillis())
    CountMetric(
      time = time,
      total = duration.getOrElse(0),
      hits = 1
    )
  }
}
