package xed.api_service.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.{JsonUtils, TimeUtils}

object SRSSource {
  val FLASHCARD = "flashcard"
  val BOT = "bot"
}

object SRSStatus {
  val Remain = "remain"
  val Learning = "learning"
  val Ignored = "ignored"
  val Done = "done"
}

case class ReviewInfo(id: String,
                      source: String,// = SRSSource.FLASHCARD,
                      deckId: Option[String],
                      cardId: Option[String],
                      username: Option[String],
                      status: Option[String],
                      memorizingLevel: Option[Int],
                      startDate: Option[Long],
                      dueDate: Option[Long],
                      lastReviewTime: Option[Long] = None,
                      updatedTime: Option[Long] = None,
                      createdTime: Option[Long] = None) extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)

  @JsonIgnore
  def isLearningCard() = status.getOrElse(SRSStatus.Learning).equalsIgnoreCase(SRSStatus.Learning)

  @JsonIgnore
  def isDoneCard() = status.getOrElse(SRSStatus.Learning).equalsIgnoreCase(SRSStatus.Done)

  def reviewRequired() = {
    val (time, _) = TimeUtils.calcBeginOfDayInMills()

    time >= dueDate.get
  }


  def asLearningModel() = {
    this.copy(
      status = Some(SRSStatus.Learning),
      lastReviewTime = Some(System.currentTimeMillis()),
      updatedTime = Some(System.currentTimeMillis())
    )
  }

  def asIgnoreModel() = {
    this.copy(
      status = Some(SRSStatus.Ignored),
      lastReviewTime = Some(System.currentTimeMillis()),
      updatedTime = Some(System.currentTimeMillis())
    )
  }

  def asDoneModel() = {
    this.copy(
      status = Some(SRSStatus.Done),
      lastReviewTime = Some(System.currentTimeMillis()),
      updatedTime = Some(System.currentTimeMillis())
    )
  }

}

case class ReviewHistoryInfo(id: String,
                             success: Option[Boolean],
                             username: Option[String],
                             deckId: Option[String],
                             cardId: Option[String],
                             memorizingLevel: Option[Int],
                             answer: Option[Int],
                             duration: Option[Int],
                             createdTime: Option[Long]) extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)
}
