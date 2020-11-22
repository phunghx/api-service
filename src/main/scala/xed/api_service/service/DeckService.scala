package xed.api_service.service

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import com.fasterxml.jackson.databind.node.TextNode
import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.twitter.util.Future
import xed.api_service.domain._
import xed.api_service.domain.category.DeckCategory
import xed.api_service.domain.exception._
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.module.BaseResponse
import xed.api_service.repository.{DeckRepository, UpdateResult}
import xed.api_service.repository.card.CardRepository
import xed.api_service.service.Operation._
import xed.api_service.util.ThreadUtil._
import xed.api_service.util.{Implicits, Utils, ZConfig}
import xed.profiler.Profiler
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer

trait DeckService {
  lazy val clazz = getClass.getSimpleName

  def search(user: SignedInUser, query: Option[String], request: SearchRequest): Future[PageResult[Deck]]

  def searchMyDeck(user: SignedInUser, request: SearchRequest): Future[PageResult[Deck]]

  def getDeck(deckId: String): Future[Deck]

  def getDecks(user: SignedInUser, deckIds: Seq[String]): Future[Seq[Deck]]

  def getDeckAsMap(user: SignedInUser, deckIds: Seq[String]): Future[Map[String, Deck]]

  def getDeckAsMap( deckIds: Seq[String]): Future[Map[String, Deck]]

  def getNewDecks(user: SignedInUser): Future[Seq[Deck]]

  def getTrendingDecks(user: SignedInUser, size: Int): Future[Seq[Deck]]

  def searchNewDecks(): Future[Seq[Deck]]

  def deleteDeck(user: SignedInUser, deckId: String): Future[Boolean]

  def temporaryDeleteDecks(user: SignedInUser, deckIds: Seq[String]): Future[Seq[String]]

  def temporaryDeleteDeck(user: SignedInUser, deckId: String): Future[Boolean]

  def unpublishDeck(user: SignedInUser, deckId: String): Future[Boolean]

  def publishDeck(user: SignedInUser, deckId: String): Future[Boolean]

  def createDeck(user: SignedInUser, request: CreateDeckRequest): Future[Deck]

  def updateDeck(user: SignedInUser, request: EditDeckRequest): Future[Boolean]

  def multiUpdate(decks: Seq[Deck]): Future[UpdateResult]

  def addCard(user: SignedInUser, request: AddCardRequest): Future[BaseResponse]

  def addMultiCards(user: SignedInUser, request: AddMultiCardRequest): Future[Seq[String]]

  def removeCards(user: SignedInUser, request: RemoveMultiCardRequest): Future[Deck]

  def getDeckCategories(): Future[Seq[DeckCategory]]

  def addCardToNewsDeck(user: SignedInUser, request: AddNewsCardRequest): Future[BaseResponse]
}

case class DeckServiceImpl (deckRepository: DeckRepository,
                                      cardRepository: CardRepository,
                                      analytics: Analytics,
                                      eventPublisher: ActorRef) extends DeckService {

  import Implicits._

  import scala.collection.JavaConversions._

  private val newsDeckId = ZConfig.getString("news_deck.id")

  private val cacheIntervalInMinutes = ZConfig.getInt("cache.new_decks.interval_in_mins", 5)

  private val newDeckCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(cacheIntervalInMinutes, TimeUnit.MINUTES)
    .build(new CacheLoader[String, Seq[Deck]] {
      override def load(key: String): Seq[Deck] = {
        searchNewDecks().sync()
      }
    })

  private val trendingDeckCache = CacheBuilder.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(cacheIntervalInMinutes, TimeUnit.MINUTES)
    .build(new CacheLoader[String, Seq[Deck]] {
      override def load(key: String): Seq[Deck] = {
        _getTrendingDecks(key.toInt).sync()
      }
    })

  override def getNewDecks(user: SignedInUser): Future[Seq[Deck]] = async {
    newDeckCache.get("new_decks")
  }

  override def getTrendingDecks(user: SignedInUser, size: Int): Future[Seq[Deck]] = async {
    trendingDeckCache.get(size.toString)
  }

  private def _getTrendingDecks(size: Int): Future[Seq[Deck]] = {
    val newRequest = SearchRequest(from = 0, size = size)
      .addIfNotExist(NotQuery("deck_status", ListBuffer(
        TextNode.valueOf(Status.PROTECTED.id.toString),
        TextNode.valueOf(Status.DELETED.id.toString)
      )))
      .addIfNotExist(SortQuery("weekly_total_likes", SortQuery.ORDER_DESC))
      .addIfNotExist(SortQuery("created_time", SortQuery.ORDER_DESC))
    deckRepository.genericSearch(newRequest).map(_.records)
  }



  private val categories = ZConfig.getMapList("deck.categories")
    .filter(e => e.containsKey("id") && e.containsKey("name"))
    .map(e => {
      DeckCategory(
        e("id").toString,
        e("name").toString)
    })

  private val sizePerPage = ZConfig.getInt("deck.size_per_page", 20)

  override def createDeck(user: SignedInUser, request: CreateDeckRequest): Future[Deck] = {
    val deck = request.build(user)
    for {
      r <- deckRepository.insert(deck)
    } yield r match {
      case Some(id) =>
        analytics.log(CREATE_DECK, user.userProfile, Map(
          "func" -> Thread.currentThread().getMethodName,
          "deck" -> deck.logInfo
        ))
        deck.copy(id = id)
      case _ => throw InternalError()
    }
  }

  override def updateDeck(user: SignedInUser, request: EditDeckRequest): Future[Boolean] = {
    val deck = request.build(user)
    for {
      oldDeck <- deckRepository.get(request.deckId).map(Utils.throwIfNotExist(_))
      r <- deckRepository.update(deck).map(_.count > 0)
    } yield {
      if (r) eventPublisher ! DeckChangedMessage(deck)
      r
    }
  }

  override def multiUpdate(decks: Seq[Deck]): Future[UpdateResult] = {
    deckRepository.multiUpdate(decks)
  }

  override def addCard(user: SignedInUser, request: AddCardRequest): Future[BaseResponse] = {
    val card = request.build(user)
    for {
      deck <- deckRepository.get(request.deckId).map(Utils.throwIfNotExist(_))
      _ = verifyPerms(user, deck)
      cardId <- cardRepository.addCard(card).map(Utils.throwIfNotExist(_))
      _ = deck.addCard(cardId)
      r <- deckRepository.insert(deck.copy(updatedTime = Some(System.currentTimeMillis())), false)
    } yield r match {
      case Some(x) => BaseResponse(true, Some(card.id), None)
      case _ => throw InternalError(Some("Can't add cards to this deck."))
    }
  }


  override def addMultiCards(user: SignedInUser, request: AddMultiCardRequest): Future[Seq[String]] = {
    val cards = request.build(user, request.deckId)
    for {
      deck <- deckRepository.get(request.deckId).map(Utils.throwIfNotExist(_))
      _ = verifyPerms(user, deck)
      addedCardIds <- cardRepository.addCards(cards)
      _ = {
        deck.updatedTime = Some(System.currentTimeMillis())
        deck.addCards(addedCardIds)
      }
      r <- deckRepository.insert(deck, false)
    } yield r match {
      case Some(x) => addedCardIds
      case _ => throw InternalError(Some("Can't add cards to this deck."))
    }
  }

  /**
   * This is a method to check perms in order to delete or edit an deck.
   *
   * @param deck
   * @param user
   * @return
   */
  private def verifyPerms(user: SignedInUser, deck: Deck) = {
    if (!deck.username.get.equals(user.username)) throw UnAuthorizedError(Some(s"Not allow to act on this deck"))
    true
  }

  override def removeCards(user: SignedInUser, request: RemoveMultiCardRequest) = {
    for {
      deck <- deckRepository.get(request.deckId).map(Utils.throwIfNotExist(_))
      _ = {
        verifyPerms(user, deck)
        deck.updatedTime = Some(System.currentTimeMillis())
        deck.removeCards(request.cardIds)
        deck.deckStatus = deck.cards match {
          case Some(ids) if ids.nonEmpty => deck.deckStatus
          case _ => Some(Status.PROTECTED.id)
        }
      }
      r <- deckRepository.update(deck)
    } yield r.count > 0 match {
      case true => deck
      case _ => throw InternalError(Some("Can't remove these cards."))
    }
  }

  override def getDeck(deckId: String): Future[Deck] = {
    for {
      deck <- deckRepository.get(deckId)
    } yield deck match {
      case Some(x) =>
        x
      case _ => throw NotFoundError(Some(s"No deck was found for id = $deckId"))
    }
  }

  override def getDecks(user: SignedInUser, deckIds: Seq[String]): Future[Seq[Deck]] = {
    for {
      deckMap <- deckRepository.multiGet(deckIds)
    } yield {
      analytics.log(VIEW_DECK, user.userProfile, Map(
        "func" -> Thread.currentThread().getMethodName,
        "deck_ids" -> deckIds
      ))
      deckIds.filter(deckMap.contains)
        .map(deckMap.get)
        .filter(_.isDefined)
        .map(_.get)
    }
  }

  override def getDeckAsMap(user: SignedInUser, deckIds: Seq[String]): Future[Map[String, Deck]] = {
    analytics.log(VIEW_DECK, user.userProfile, Map(
      "func" -> Thread.currentThread().getMethodName,
      "deck_ids" -> deckIds
    ))
    deckRepository.multiGet(deckIds)
  }

  override def getDeckAsMap(deckIds: Seq[String]): Future[Map[String, Deck]] = Profiler(s"$clazz.getDeckAsMap") {
    deckIds.grouped(50).foldLeft(Future.value(Map.empty[String, Deck]))((fn, deckIds) => {
      fn flatMap { deckMap =>
        deckRepository.multiGet(deckIds).map(r => deckMap ++ r)

      }
    })
  }

  override def unpublishDeck(user: SignedInUser, deckId: String): Future[Boolean] = {
    for {
      deck <- deckRepository.get(deckId).map(Utils.throwIfNotExist(_))
      _ = verifyPerms(user, deck)
      newDeck = deck.copy(
        deckStatus = Some(Status.PROTECTED.id),
        updatedTime = Some(System.currentTimeMillis())
      )
      r <- deckRepository.update(newDeck).map(_.count > 0)
    } yield {
      if (r) {
        analytics.log(UNPUBLISHED_DECK, user.userProfile, Map(
          "func" -> Thread.currentThread().getMethodName,
          "deck" -> newDeck
        ))
        eventPublisher ! DeckStatusChangedMessage(deckId, Status.PROTECTED.id)
      }
      r
    }
  }

  override def publishDeck(user: SignedInUser, deckId: String): Future[Boolean] = {
    for {
      deck <- deckRepository.get(deckId).map(Utils.throwIfNotExist(_))
      _ = verifyPerms(user, deck)
      newDeck = deck.copy(
        deckStatus = Some(Status.PUBLISHED.id),
        updatedTime = Some(System.currentTimeMillis()),
        createdTime = Some(System.currentTimeMillis())
      )
      r <- deckRepository.update(newDeck).map(_.count > 0)
    } yield {
      if (r) {
        analytics.log(PUBLISH_DECK, user.userProfile, Map(
          "func" -> Thread.currentThread().getMethodName,
          "deck" -> newDeck
        ))
        eventPublisher ! DeckStatusChangedMessage(deckId, Status.PUBLISHED.id)
      }
      r
    }
  }

  override def temporaryDeleteDeck(user: SignedInUser, deckId: String): Future[Boolean] = {
    for {
      r <- deckRepository.get(deckId).flatMap({
        case Some(deck) =>
          verifyPerms(user, deck)
          val newDeck = deck.copy(
            deckStatus = Some(Status.DELETED.id),
            updatedTime = Some(System.currentTimeMillis())
          )
          deckRepository.update(newDeck).map(_.count > 0)
        case _ => Future.True
      })
    } yield {
      if (r) eventPublisher ! DeckStatusChangedMessage(deckId, Status.DELETED.id)
      r
    }
  }

  override def temporaryDeleteDecks(user: SignedInUser, deckIds: Seq[String]): Future[Seq[String]] = {
    for {
      decks <- deckRepository.multiGet(deckIds)
      updateDecks = decks.values.map(deck => deck.copy(
        deckStatus = Some(Status.DELETED.id),
        updatedTime = Some(System.currentTimeMillis())
      )).toSeq

      deletedIds <- if (updateDecks.isEmpty) Future.value(Set.empty[String]) else {
        deckRepository.multiUpdate(updateDecks).map(r => {
          r.data
            .getOrElse(Map.empty[String, Boolean])
            .filter(_._2).keySet
        })
      }

      r = deckIds.filter(deckId => {
        !decks.contains(deckId) || deletedIds.contains(deckId)
      })

    } yield {
      deletedIds.foreach(deletedId => {
        eventPublisher ! DeckStatusChangedMessage(deletedId, Status.DELETED.id)
      })

      r
    }
  }

  override def deleteDeck(user: SignedInUser, deckId: String): Future[Boolean] = {
    for {
      deck <- deckRepository.get(deckId).map(Utils.throwIfNotExist(_))
      _ = verifyPerms(user, deck)
      r <- deckRepository.delete(deckId)
    } yield {
      if (r) {
        analytics.log(DELETE_DECK, user.userProfile, Map(
          "func" -> Thread.currentThread().getMethodName,
          "deck" -> deck.logInfo
        ))
        eventPublisher ! DeckStatusChangedMessage(deckId, Status.DELETED.id)
      }
      r
    }
  }

  override def searchNewDecks(): Future[Seq[Deck]] = {
    val newRequest = SearchRequest(from = 0, size = sizePerPage)
      .addIfNotExist(TermsQuery("deck_status", ListBuffer(TextNode.valueOf(Status.PUBLISHED.id.toString))))
      .addIfNotExist(SortQuery("created_time", SortQuery.ORDER_DESC))
      .addIfNotExist(SortQuery("updated_time", SortQuery.ORDER_DESC))

    deckRepository.genericSearch(newRequest).map(_.records)
  }

  override def search(user: SignedInUser, query: Option[String], request: SearchRequest): Future[PageResult[Deck]] = {

    val newRequest = request
      .addIfNotExist(TermsQuery("deck_status", ListBuffer(TextNode.valueOf(Status.PUBLISHED.id.toString))))
      .addIfNotExist(SortQuery("created_time", SortQuery.ORDER_DESC))
      .addIfNotExist(SortQuery("updated_time", SortQuery.ORDER_DESC))
    analytics.log(SEARCH_DECK, user.userProfile, Map(
      "func" -> Thread.currentThread().getMethodName,
      "query" -> query,
      "request" -> request
    ))
    deckRepository.search(query, newRequest)
  }

  override def searchMyDeck(user: SignedInUser, request: SearchRequest): Future[PageResult[Deck]] = {
    val newRequest = request
      .removeField("username")
      .removeField("deck_status")
      .addIfNotExist(TermsQuery("username", ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(NotQuery("deck_status", ListBuffer(TextNode.valueOf(Status.DELETED.id.toString))))
      .addIfNotExist(SortQuery("created_time", SortQuery.ORDER_DESC))
      .addIfNotExist(SortQuery("updated_time", SortQuery.ORDER_DESC))
    deckRepository.genericSearch(newRequest)
  }

  override def getDeckCategories(): Future[Seq[DeckCategory]] = Future {
    categories
  }

  override def addCardToNewsDeck(user: SignedInUser, request: AddNewsCardRequest): Future[BaseResponse] = {
    val deckId = getNewsDeckId(user.username)

    deckRepository.get(deckId).flatMap {
      case Some(x) if (x.deckStatus.exists(_ != Status.DELETED.id)) => Future.True
      case _ =>
        val deck = Deck.createNewsDeck(deckId, user.username)
        deckRepository.insert(deck, isCreated = false).map(maybeDeckId => {
          analytics.log(CREATE_DECK, user.userProfile, Map(
            "func" -> Thread.currentThread().getMethodName,
            "deck" -> deck.logInfo
          ))
          maybeDeckId.isDefined
        })
    }.flatMap(success => {
      addCard(user, AddCardRequest(deckId, request.cardVersion, request.design))
    })
  }

  def getNewsDeckId(username: String): String = newsDeckId.format(username)
}