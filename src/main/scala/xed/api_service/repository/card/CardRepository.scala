package xed.api_service.repository.card
import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.api_service.domain.Card


trait CardRepository extends Logging {
  lazy val clazz = getClass.getSimpleName

  def addCard(card: Card): Future[Option[String]]

  def addCards(cards: Seq[Card]): Future[Seq[String]]

  def getCard(cardId: String): Future[Option[Card]]

  def getCards(cardIds: Seq[String]): Future[Seq[Card]]

  def getCardAsMap(cardIds: Seq[String]): Future[Map[String, Card]]

  def updateCard(card: Card): Future[Boolean]

  def deleteCard(cardId: String): Future[Boolean]

  def deleteCards(cardIds: Seq[String]): Future[Boolean]
}