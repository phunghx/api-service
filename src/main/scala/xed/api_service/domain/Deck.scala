package xed.api_service.domain

import xed.api_service.domain.design.Container
import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.{JsonUtils, ZConfig}
import xed.userprofile.domain.ShortUserProfile

import scala.collection.mutable.ListBuffer

case class Deck(id: String,
                username: Option[String],
                name: Option[String],
                thumbnail: Option[String],
                description: Option[String],
                design: Option[Container],
                var cards: Option[ListBuffer[String]],
                category: Option[String] = None,
                totalLikes: Option[Int] = None,
                weeklyTotalLikes: Option[Int] = None,
                var deckStatus: Option[Int] = None,
                var updatedTime: Option[Long] = None,
                var createdTime: Option[Long] = None,
                var ownerDetail: Option[ShortUserProfile] = None) extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)

  def addCard(cardId: String) = {
    cards match {
      case Some(cards) => cards.append(cardId)
      case _ =>
        cards = Some(ListBuffer[String](cardId))
    }
    this
  }

  def addCards(cardIds: Seq[String]) = {
    cards match {
      case Some(cards) =>
        cards.appendAll(cardIds)
      case _ =>
        cards = Some(ListBuffer.apply[String](cardIds:_*))
    }
    this
  }

  def removeCard(cardId: String) =  {
    cards match {
      case Some(x) =>  cards = Some(x.filterNot(_.equals(cardId)))
      case _ =>
    }
    this
  }

  def removeCards(cardIds: Seq[String]) =  {
    cards match {
      case Some(x) =>  cards = Some(x.filterNot(cardIds.contains(_)))
      case _ =>
    }
    this
  }

  def logInfo: Map[String, Any] = {
    Map(
      "id" -> id,
      "name" -> name,
      "category" -> category,
      "status" -> deckStatus,
      "created_time" -> createdTime
    )
  }
}

object Deck {
  private val name = ZConfig.getString("news_deck.name", "")
  private val description = ZConfig.getString("news_deck.description", "")

  def createNewsDeck(id: String, username: String): Deck = {
    Deck(
      id = id,
      username = Some(username),
      name = Some(name),
      description = Some(description),
      thumbnail = None,
      category = None,
      design = None,
      cards = Some(ListBuffer.empty[String]),
      deckStatus = Some(Status.PROTECTED.id),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}
