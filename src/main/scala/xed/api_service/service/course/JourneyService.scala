package xed.api_service.service.course

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.node.TextNode
import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.twitter.util.Future
import xed.api_service.domain.Status
import xed.api_service.domain.course.JourneyInfo
import xed.api_service.domain.exception.{InternalError, NotFoundError, UnAuthorizedError}
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.repository.{DeckRepository, JourneyRepository}
import xed.api_service.util.{Implicits, ZConfig}
import xed.userprofile.SignedInUser

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ListBuffer


trait JourneyService {

  def add(journey: JourneyInfo): Future[JourneyInfo]

  def multiAdd(journeys: Seq[JourneyInfo]):  Future[Seq[JourneyInfo]]

  def create(user: SignedInUser, request: CreateJourneyRequest): Future[JourneyInfo]

  def update(user: SignedInUser, request: UpdateJourneyRequest): Future[Boolean]

  def get(journeyId: String): Future[Option[JourneyInfo]]

  def multiGet(journeyIds: Seq[String]) : Future[Seq[JourneyInfo]]

  def delete(user: SignedInUser,journeyId: String): Future[Boolean]

  def publish(user: SignedInUser, request: PublishJourneyRequest): Future[JourneyInfo]

  def search(searchRequest: SearchRequest): Future[PageResult[JourneyInfo]]

  def searchMyJourneys(user: SignedInUser, searchRequest: SearchRequest): Future[PageResult[JourneyInfo]]

}


case class JourneyServiceWithCache(service: JourneyService) extends JourneyService {
  import Implicits._

  import scala.collection.JavaConversions._

  private val cacheMaxSize = ZConfig.getInt("cache.max_size", 1000)
  private val cacheIntervalInMinutes = ZConfig.getInt("cache.interval_in_mins", 120)

  private val cache = CacheBuilder.newBuilder()
    .maximumSize(cacheMaxSize)
    .expireAfterWrite(cacheIntervalInMinutes, TimeUnit.MINUTES)
    .build(new CacheLoader[String, Option[JourneyInfo]] {
      override def load(key: String): Option[JourneyInfo] = {
        service.get(journeyId = key).sync()
      }
    })

  override def create(user: SignedInUser, request: CreateJourneyRequest) = service.create(user, request)
  override def add(journey: JourneyInfo) = service.add(journey)
  override def multiAdd(journeys: Seq[JourneyInfo])  = service.multiAdd(journeys)

  override def get(journeyId: String): Future[Option[JourneyInfo]] = async {
    cache.get(journeyId)
  }

  override def multiGet(journeyIds: Seq[String]): Future[Seq[JourneyInfo]] = async {
    val journeyMap = cache.getAll(journeyIds.asJava)
    journeyIds.filter(journeyMap.contains(_))
      .map(journeyMap.get)
      .filter(_.isDefined)
      .map(_.get)
  }

  override def delete(user: SignedInUser, journeyId: String): Future[Boolean] = {
    service.delete(user, journeyId).map(r =>{
      cache.invalidate(journeyId)
      r
    })
  }


  override def update(user: SignedInUser, request: UpdateJourneyRequest): Future[Boolean] = {

    service.update(user, request).map(r =>{
      cache.invalidate(request.journeyId)
      r
    })
  }

  override def publish(user: SignedInUser, request: PublishJourneyRequest): Future[JourneyInfo] = {
    service.publish(user, request).map(r =>{
      cache.invalidate(request.journeyId)
      r
    })
  }

  override def search(searchRequest: SearchRequest) = service.search(searchRequest)

  override def searchMyJourneys(user: SignedInUser, searchRequest: SearchRequest) = service.searchMyJourneys(user, searchRequest)
}


case class JourneyServiceImpl(deckRepository: DeckRepository,
                                        journeyRepository: JourneyRepository
                                       ) extends JourneyService {


  override def create(user: SignedInUser, request: CreateJourneyRequest): Future[JourneyInfo] = {
    val journey = request.build(user)
    for {
      decks <- deckRepository.multiGet(journey.deckIds.getOrElse(Nil))
      _ = {
        if(decks.size != journey.deckIds.map(_.size).getOrElse(0))
          throw InternalError(Some("the decks are invalid."))
      }
      r <- add(journey)
    } yield r
  }

  /** *
    * Dummy Version, Not Lock, Not Check currentVersion is valid
    *
    * @param journey
    * @return
    */
  override def add(journey: JourneyInfo): Future[JourneyInfo] = {
    for {
      r <- journeyRepository.insert(journey, false)
    } yield r.isDefined match {
      case true => journey
      case _ => throw InternalError(Some(s"Can't create this journey."))
    }
  }

  override def multiAdd(journeys: Seq[JourneyInfo]): Future[Seq[JourneyInfo]] = {
    journeyRepository.multiInsert(journeys,false)
  }

  override def get(journeyId: String): Future[Option[JourneyInfo]] = {
    journeyRepository.get(journeyId)
  }

  override def multiGet(journeyIds: Seq[String]): Future[Seq[JourneyInfo]] = {
    for {
      journeyMap <- journeyRepository.multiGet(journeyIds)
    } yield {
      journeyIds.filter(journeyMap.contains)
        .map(journeyMap.get)
        .filter(_.isDefined)
        .map(_.get)
    }
  }

  override def delete(user: SignedInUser, journeyId: String): Future[Boolean] = {
     for {
       journey <- journeyRepository.get(journeyId).map(throwIfNotExist(_))
       _ = checkPerms(user,journey)
       r<- journeyRepository.delete(journeyId)
     } yield r
  }


  override def update(user: SignedInUser, request: UpdateJourneyRequest): Future[Boolean] = {
    for {
      journey <- journeyRepository.get(request.journeyId).map(throwIfNotExist(_))
      _ = checkPerms(user,journey)
      newJourney = request.build(user)
      r <- journeyRepository.update(newJourney).map(_.count > 0)
    } yield r
  }

  override def publish(user: SignedInUser, request: PublishJourneyRequest): Future[JourneyInfo] = {
    for {
      journey <- journeyRepository.get(request.journeyId).map(throwIfNotExist(_))
      _ = checkPerms(user,journey)
      newJourney = journey.copy(
        status = Some(Status.PUBLISHED.id),
        updatedTime = Some(System.currentTimeMillis())
      )
      r <- journeyRepository.update(newJourney).map(_.count > 0)
    } yield r match {
      case true => newJourney
      case _ => throw InternalError(Some("Can't publish this journey."))
    }
  }

  override def search(searchRequest: SearchRequest): Future[PageResult[JourneyInfo]] = {
    journeyRepository.genericSearch(searchRequest)
  }

  override def searchMyJourneys(user: SignedInUser, searchRequest: SearchRequest): Future[PageResult[JourneyInfo]] = {
    val newRequest = searchRequest
      .removeField("owner")
      .addIfNotExist(TermsQuery("owner",ListBuffer(TextNode.valueOf(user.username))))
      .addIfNotExist(SortQuery("created_time",SortQuery.ORDER_DESC))
    journeyRepository.genericSearch(newRequest)
  }

  private def throwIfNotExist[T](v : Option[T], msg: Option[String] = None) = v match  {
    case Some(x) => x
    case _ => throw NotFoundError(msg)
  }

  private def checkMyJourney(user: SignedInUser, journey: JourneyInfo) = {
    journey.creator match {
      case Some(owner) if owner.equals(user.username)  => true
      case _ => throw UnAuthorizedError(Some(s"No permission to act on this journey."))
    }
  }


  private def checkPerms(user: SignedInUser,journeyInfo: JourneyInfo) = {
    journeyInfo.creator match {
      case Some(owner) if !owner.equals(user.username) => throw UnAuthorizedError(Some(s"No permission to act on this journey."))
      case _ => true
    }
  }

}







