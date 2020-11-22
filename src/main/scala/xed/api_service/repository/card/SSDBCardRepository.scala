package xed.api_service.repository.card


import com.twitter.util.Future
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.Card
import xed.api_service.util.{Implicits, JsonUtils}

import scala.collection.JavaConversions._


case class SSDBCardRepository(ssdb: SSDB, keyName: String) extends  CardRepository {


  override def addCard(card: Card): Future[Option[String]] = Implicits.async {
    val r = ssdb.hset(keyName,card.id, JsonUtils.toJson(card,false))
    if(r.ok()) {
      Option(card.id)
    }else None
  }

  override def addCards(cards: Seq[Card]): Future[Seq[String]] = Implicits.async {

    val r = ssdb.multi_hset(
      keyName,
      cards.flatMap(card => Seq(card.id, JsonUtils.toJson(card,false))):_*
    )
    if(r.ok())
      cards.map(_.id)
    else
      Seq.empty
  }

  override def updateCard(card: Card): Future[Boolean] = Implicits.async {
    val r = ssdb.hset(keyName,card.id, JsonUtils.toJson(card,false))
    r.ok()
  }


  def getCard(cardId: String): Future[Option[Card]] = Implicits.async {
    val r = ssdb.hget(keyName, cardId)
    if(r.ok()) {
      Option(JsonUtils.fromJson[Card](r.asString()))
    } else None
  }

  def getCards(cardIds: Seq[String]): Future[Seq[Card]] = {
    for {
      cardMap <- getCardAsMap(cardIds)
    } yield {
      cardIds.map(cardMap.get)
        .filter(_.isDefined)
        .map(_.get)
    }
  }

  def getCardAsMap(cardIds: Seq[String]): Future[Map[String, Card]] = Implicits.async {
    val r = ssdb.multi_hget(keyName, cardIds: _*)
    if (r.ok()) {
      r.listString()
        .grouped(2)
        .map(e => e(0) -> e(1))
        .filter(_._2 != null)
        .filter(_._2.nonEmpty)
        .map(e => e._1 -> JsonUtils.fromJson[Card](e._2))
        .toMap
    } else Map.empty

  }


  def getAllCardIds(size: Int): Future[Seq[String]] = Implicits.async {
    val r = ssdb.hkeys(keyName, "", "",size)
    if (r.ok()) {
      r.listString()
    } else Seq.empty

  }



  def deleteCard(cardId: String): Future[Boolean] = Implicits.async {
    val r = ssdb.hdel(keyName, cardId)
    r.ok()
  }

  def deleteCards(cardIds: Seq[String]): Future[Boolean] = Implicits.async {
    val r = ssdb.multi_hdel(keyName, cardIds:_*)
    r.ok()
  }

}
