package xed.api_service.service

import akka.actor.ActorRef
import com.fasterxml.jackson.databind.JsonNode
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.{Inject, Named}
import xed.api_service.domain._
import xed.api_service.domain.exception._
import xed.api_service.domain.request.EditCardRequest
import xed.api_service.module.BaseResponse
import xed.api_service.repository.card.CardRepository
import xed.api_service.repository.{DeckRepository, SRSRepository}
import xed.api_service.util.{JsonUtils, Utils}
import xed.profiler.Profiler
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer

trait CardService extends  Logging {
  lazy val clazz = getClass.getSimpleName

  def getCard(cardId: String): Future[Card]

  def getCards(cardIds: Seq[String]) : Future[Seq[Card]]

  def getQACards(cardIds: Seq[String]) : Future[Seq[Card]]

  def getQACardIds(cardIds: Seq[String]) : Future[Seq[String]]

  def getCardAsMap(cardIds: Seq[String]) : Future[Map[String,Card]]

  def getReviewCardAsList(user: SignedInUser,source: String, cardIds: Seq[String]) : Future[Seq[SRSCard]]

  def getReviewCardAsMap(user: SignedInUser,source: String, cardIds: Seq[String]) : Future[Map[String,Seq[SRSCard]]]

  def editCard(user: SignedInUser, request: EditCardRequest): Future[Card]

  def deleteCard(getUser: SignedInUser, cardId: String): Future[BaseResponse]
}



case class CardServiceImpl@Inject()(deckRepository: DeckRepository,
                                    cardRepository: CardRepository,
                                    srsRepository: SRSRepository,
                                    @Named("event-publisher-router") deckApiRouterRef: ActorRef) extends CardService {

  override def deleteCard(user: SignedInUser, cardId: String): Future[BaseResponse] = {
    for {
      card <- cardRepository.getCard(cardId).map(Utils.throwIfNotExist(_))
      deck <- deckRepository.get(card.deckId.get)
      r <- deck match {
        case Some(deck) =>
          verifyPerms(user,deck)
          deck.removeCard(cardId)
          deck.updatedTime = Some(System.currentTimeMillis())
          deckRepository.update(deck).map(_.count > 0)
        case _ => Future.True
      }
      _ <- if(r) cardRepository.deleteCard(cardId) else Future.False
    } yield {
      BaseResponse(r,None, None)
    }
  }

  /**
    * This is a method to check perms in order to delete or edit a deck.
    * @param deck
    * @param user
    * @return
    */
  private def verifyPerms(user: SignedInUser, deck: Deck) = {
    if(!deck.username.get.equals(user.username))
      throw UnAuthorizedError(Some(s"Not allow to act on this card"))
    true
  }

  private def verifyCardPerms(user: SignedInUser, card: Card) = {
    if(!card.username.get.equals(user.username))
      throw UnAuthorizedError(Some(s"Not allow to act on this card"))
    true
  }

  override def getCard(cardId: String): Future[Card] = {
    for {
      card <- cardRepository.getCard( cardId)
    } yield card match {
      case Some(x) => x
      case _ => throw NotFoundError()
    }
  }

  override def getCards(cardIds: Seq[String]): Future[Seq[Card]] = {
    cardRepository.getCards(cardIds)
  }

  override def getQACards(cardIds: Seq[String]): Future[Seq[Card]] = {
    cardRepository.getCards(cardIds).map(cards => {
      cards.filter(_.cardView.isDefined)
        .filter(_.cardView.get.isQuestionAnswerCard())
    })
  }

  override def getQACardIds(cardIds: Seq[String]): Future[Seq[String]] = {
    cardRepository.getCards(cardIds).map(cards => {
      cards.filter(_.cardView.isDefined)
        .filter(_.cardView.get.isQuestionAnswerCard())
        .map(_.id)
    })
  }

  override def getCardAsMap(cardIds: Seq[String]): Future[Map[String, Card]] = Profiler(s"$clazz.getCardAsMap") {
    cardRepository.getCardAsMap( cardIds)
  }

  override def getReviewCardAsList(user: SignedInUser, source: String, cardIds: Seq[String]): Future[Seq[SRSCard]] = {
    for {
      cardMap <- cardRepository.getCardAsMap(cardIds)
      validCardIds = cardIds.filter(cardMap.contains)
      modelMap <- srsRepository.getReviewInfos(user.username,source, validCardIds)
        .map(_.map(x => x._2.cardId.get -> x._2))
    } yield {
      validCardIds.map(cardId => SRSCard(
        cardId,
        card = cardMap.get(cardId).get,
        model = modelMap.get(cardId)
      ))
    }
  }

  override def getReviewCardAsMap(user: SignedInUser,source: String, cardIds: Seq[String]): Future[Map[String, Seq[SRSCard]]] = {
    for {
      cardMap <- cardRepository.getCardAsMap(cardIds)
      validCardIds = cardIds.filter(cardMap.contains)
      modelMap <- srsRepository.getReviewInfos(user.username,source, validCardIds)
        .map(_.map(x => x._2.cardId.get -> x._2))
    } yield {
      val remainCards = new ListBuffer[SRSCard]()
      val learningCards = new ListBuffer[SRSCard]()
      val doneCards = new ListBuffer[SRSCard]()
      
      validCardIds.map(cardId => {
        val srsCard = SRSCard(
          cardId,
          card = cardMap.get(cardId).get,
          model = modelMap.get(cardId)
        )

        if(srsCard.model.isEmpty)
          remainCards.append(srsCard)
        else if(srsCard.model.map(_.isLearningCard()).getOrElse(false))
          learningCards.append(srsCard)
        else
          doneCards.append(srsCard)
      })

      Map(
        SRSStatus.Remain -> remainCards,
        SRSStatus.Learning -> learningCards,
        SRSStatus.Done -> doneCards
      )
    }
  }

  override def editCard(user: SignedInUser, request: EditCardRequest): Future[Card] = {

    for {
      oldCard <- cardRepository.getCard(request.cardId).map(Utils.throwIfNotExist(_))
      _ = verifyCardPerms(user,oldCard)
      card = oldCard.copy(
        design = request.design.map(JsonUtils.toNode[JsonNode](_)),
        updatedTime = Some(System.currentTimeMillis())
      )
      r <- cardRepository.updateCard(card)
    } yield r match {
      case true => card
      case _ => throw InternalError()
    }
  }

}
