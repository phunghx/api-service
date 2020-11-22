package xed.api_service.repository.card

import com.twitter.util.Future
import org.elasticsearch.client.transport.TransportClient
import xed.api_service.domain.Card
import xed.api_service.repository.{AbstractESRepository, ESConfig}

case class ESCardRepository(client: TransportClient,
                            config: ESConfig,
                            esType: String) extends AbstractESRepository[Card] with CardRepository {


  override def addCard(card: Card): Future[Option[String]] = {
    insert(card, true)
  }

  override  def addCards(cards: Seq[Card]): Future[Seq[String]] = {
    multiInsert(cards).map(_.map(i => i.id))
  }

  def getCard(cardId: String): Future[Option[Card]] = {
    get(cardId)
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

  def getCardAsMap(cardIds: Seq[String]): Future[Map[String, Card]] = {
    multiGet( cardIds)
  }


  override def updateCard(card: Card): Future[Boolean] = {
    update(card).map(_.count > 0)
  }

  def deleteCard(cardId: String): Future[Boolean] = {
    delete(cardId)
  }

  override def deleteCards(cardIds: Seq[String]): Future[Boolean] = {
    multiDelete(cardIds).map(_.count > 0)
  }

}
