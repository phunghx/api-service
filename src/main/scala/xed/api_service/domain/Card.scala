package xed.api_service.domain

import java.util.concurrent.ThreadLocalRandom

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import xed.api_service.domain.design.Container
import xed.api_service.domain.design.v100.Panel
import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.{JsonUtils, TimeUtils}

import scala.concurrent.duration.DurationInt

case class Card(id: String,
                name: Option[String],
                deckId: Option[String],
                username: Option[String],
                cardVersion: Option[Int],
                design: Option[JsonNode],
                var updatedTime: Option[Long] = None,
                var createdTime: Option[Long] = None)
  extends ElasticsearchObject {


  @JsonIgnore
  lazy val cardView = design.map(JsonUtils.fromNode[CardDesign](_))

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)

  def hasFront : Boolean =  frontCount() > 0

  def frontCount(): Int = {
    import scala.collection.JavaConversions._
    design.map(
      _.at("/fronts")
        .elements()
        .size
    ).getOrElse(0)
  }

  def backCard() = {
    cardView
      .flatMap(_.back)
      .getOrElse(Panel(components = Seq.empty))
  }

  def frontAt(index: Int) = {
    cardView.flatMap(_.fronts lift index)
  }

}

case class SRSCard(cardId: String,
                   card: Card,
                   model: Option[ReviewInfo] = None) {


  @JsonIgnore
  def getFrontRandomly(): (Option[Container], Int)  = {
    getFrontAt(None)
  }

  @JsonIgnore
  def getFrontAt(frontIndex: Option[Int]) : (Option[Container], Int) = {

    val frontCount = card.frontCount()
    val index = frontIndex match {
      case Some(index) => index % frontCount
      case _ =>
        if(frontCount > 0) ThreadLocalRandom.current().nextInt(frontCount) else 0
    }

    val container = card.frontAt(index)
    (container,index)
  }

  @JsonIgnore
  def getDueDateAsReadableStr(): String = {
    val beginTime = TimeUtils.calcBeginOfDayInMills()._1
    val endTime = TimeUtils.calcBeginOfDayInMillsFrom(
      model
        .flatMap(_.dueDate)
        .getOrElse(System.currentTimeMillis())
    )._1

    val delta = (endTime - beginTime) / 1.days.toMillis

    if(delta <= 1) "tomorrow" else s"in the next $delta days"
  }


}


case class GeneratedCardResponse(word: String,
                                 partOfSpeech: String,
                                 design:CardDesign)