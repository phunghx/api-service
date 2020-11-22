package xed.api_service.service

import akka.actor.{Actor, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import com.twitter.inject.Logging
import redis.clients.jedis.JedisPool
import xed.api_service.domain.Deck
import xed.api_service.repository.{DeckRepository, SRSRepository}
import xed.api_service.repository.card.CardRepository
import xed.api_service.service.statistic.CountService
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.{Implicits, JsonUtils, ZConfig}
import xed.profiler.Profiler

case class ReviewUpdatedMessage(source: String,
                                username: String,
                                id: String)

case class ReviewAddedMessage(source: String,
                              username: String,
                              id: String)

case class ReviewRemovedMessage(source: String,
                                username: String,
                                id: String)

case class DeckChangedMessage(deck: Deck)
case class DeckStatusChangedMessage(deckId: String, status: Int)
case class DeleteCardMessage(deckId: String)

case class EventPublisher(redisPool: JedisPool,
                          deckRepository: DeckRepository,
                          cardRepository: CardRepository,
                          srsRepository: SRSRepository,
                          countService: CountService) extends Actor with Logging {

  var router = {
    val routees = Vector.fill(4) {
      val r = context.actorOf(Props(createWorker))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case Terminated(routee) =>
      router = router.removeRoutee(routee)
      val r = context.actorOf(Props(createWorker))
      context watch r
      router = router.addRoutee(r)
    case x => router.route(x, sender())
  }

  def createWorker = EventWorker(redisPool,
    deckRepository,
    cardRepository,
    srsRepository,
    countService)
}


case class EventWorker(redisPool: JedisPool,
                       deckRepository: DeckRepository,
                       cardRepository: CardRepository,
                       srsRepository: SRSRepository,
                       countService: CountService) extends Actor with Logging {
  val clazz = getClass.getSimpleName

  val deckStatusChangedChannel = ZConfig.getString("redis.deck_status_changed_channel")
  val reviewChangedChannel = ZConfig.getString("redis.review_changed_channel")

  override def receive: Receive = {
    case ReviewAddedMessage(context, username, id) => publishReviewAdded(context, username, id)
    case ReviewUpdatedMessage(context, username, id) => publishReviewUpdated(context, username, id)
    case ReviewRemovedMessage(context, username, id) => publishReviewRemoved(context, username, id)
    case DeckChangedMessage(deck) =>
      deck.deckStatus.foreach(publishDeckStatusChanged(deck.id, _))
    case DeckStatusChangedMessage(deckId, status) => publishDeckStatusChanged(deckId, status)
    case DeleteCardMessage(deckId) => deleteCards(deckId)
    case x => logger.error(s"Received an unknown message: $x")
  }

  def publishDeckStatusChanged(deckId: String, status: Int): Unit = Profiler(s"$clazz.publishDeckStatusChanged") {
    val changedEvent = JsonUtils.toJson(Map(
      "object_type" -> "deck",
      "object_id" -> deckId,
      "status" -> status
    ), false)

    Implicits.tryWith(redisPool.getResource) {
      client => {
        val id = client.publish(
          deckStatusChangedChannel,
          changedEvent
        )

        logger.debug(s"Published #${id} - ${changedEvent}")
      }
    }
  }

  def publishReviewAdded(context: String,
                         username: String,
                         id: String): Unit = Profiler(s"$clazz.publishReviewAdded") {
    updateDueDateCounter(username)
  }

  def publishReviewUpdated(context: String,
                           username: String,
                           id: String): Unit = Profiler(s"$clazz.publishReviewUpdated") {
    updateDueDateCounter(username)
  }

  def publishReviewRemoved(context: String,
                           username: String,
                           id: String): Unit = Profiler(s"$clazz.publishReviewRemoved") {
    updateDueDateCounter(username)
  }

  private def updateDueDateCounter(username: String): Unit = {
    srsRepository.getTotalDueCardDetail(username).flatMap(dataMap => {
      countService.updateDueCardCount(username, dataMap)
    }).sync()
  }

  private def deleteCards(deckId: String): Unit = {

  }

}