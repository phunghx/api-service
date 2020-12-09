package xed.api_service.service.course

import com.fasterxml.jackson.databind.node.TextNode
import com.twitter.util.logging.Logging
import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.Status
import xed.api_service.domain.course.{CourseInfo, LearningCourseInfo}
import xed.api_service.domain.exception.{InternalError, UnAuthorizedError}
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.repository.{CourseRepository, DeckRepository}
import xed.api_service.util.Utils
import xed.chatbot.repository.CourseLearningRepository
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SignedInUser, UserProfileService}

import scala.collection.mutable.ListBuffer


trait CourseService {
  def getAvailableCourses(from: Int, size: Int) : Future[PageResult[CourseInfo]]

  def create(user: SignedInUser, request: CreateCourseRequest): Future[CourseInfo]

  def add(course: CourseInfo): Future[CourseInfo]

  def update(user: SignedInUser, request: UpdateCourseRequest): Future[Boolean]

  def get(courseId: String): Future[Option[CourseInfo]]

  def multiGet(courseIds: Seq[String]): Future[Seq[CourseInfo]]

  def delete(user: SignedInUser, courseId: String): Future[Boolean]

  def search(searchRequest: SearchRequest): Future[PageResult[CourseInfo]]

  def getMyCourses(user: SignedInUser): Future[PageResult[LearningCourseInfo]]

  def getCardIds(courseInfo: CourseInfo) : Future[Seq[String]]
}


case class CourseServiceImpl(repository: CourseRepository,
                             deckRepository: DeckRepository,
                             historyRepository: CourseLearningRepository,
                             userProfileService: UserProfileService
                            ) extends CourseService with Logging {


  override def create(user: SignedInUser, request: CreateCourseRequest): Future[CourseInfo] = {
    val courseInfo = request.build(user)
    deckRepository.getNotExistIds(courseInfo.journeyIds.getOrElse(Nil)).flatMap(notExistIds => {
      if (notExistIds.isEmpty) {
        add(courseInfo)
      } else {
        throw InternalError(Some("the decks are invalid."))
      }
    })
  }

  override def add(course: CourseInfo): Future[CourseInfo] = {
    for {
      r <- repository.insert(course, false)
    } yield r.isDefined match {
      case true => course
      case _ => throw InternalError(Some(s"Can't create this course."))
    }
  }

  override def get(courseId: String): Future[Option[CourseInfo]] = {
    repository.get(courseId)
  }

  override def multiGet(courseIds: Seq[String]): Future[Seq[CourseInfo]] = {
    for {
      r <- repository.multiGet(courseIds)
    } yield {
      courseIds.map(r.get)
        .filter(_.isDefined)
        .map(_.get)
    }
  }


  override def delete(user: SignedInUser, courseId: String): Future[Boolean] = {
     for {
       course <- repository.get(courseId).map(Utils.throwIfNotExist(_))
       _ = validatePerms(user,course)
       r<- repository.delete(courseId)
     } yield r
  }


  override def update(user: SignedInUser, request: UpdateCourseRequest): Future[Boolean] = {
    for {
      course <- repository.get(request.id).map(Utils.throwIfNotExist(_))
      _ = validatePerms(user,course)
      newJourney = request.build(user)
      r <- repository.update(newJourney).map(_.count > 0)
    } yield r
  }

  override def search(searchRequest: SearchRequest): Future[PageResult[CourseInfo]] = {
    repository.genericSearch(searchRequest.addIfNotExist(
      SortQuery("created_time", SortQuery.ORDER_DESC)
    ).addIfNotExist(TermsQuery("status", ListBuffer(TextNode.valueOf(Status.PUBLISHED.id.toString)))))
  }

  def getCourseInfos(courseId: Seq[String]): Future[Seq[CourseInfo]] = {
    repository.multiGet(courseId).map(_.values.toSeq)
  }



  /**
   * Temporarily get the first 1000 courses
   * TODO: Improve mycourse ordering by the number of completed cards
   */
  override def getMyCourses(user: SignedInUser): Future[PageResult[LearningCourseInfo]] = {

    //val courseIds = historyRepository.getLearningCourses(username)
    for {
      courseList <- getAvailableCourses(0,1000).map(_.records)
      //courses <- getCourseInfos(courseIds)
      totalCompletedCardMap = historyRepository.getTotalDoneCardByCourse(user.username, courseList.map(_.id))
      learningCourses = courseList.map(courseInfo => {
        val completedCardCount = totalCompletedCardMap.getOrElse(courseInfo.id, 0)
        LearningCourseInfo(
          id = courseInfo.id,
          totalSection = courseInfo.journeyIds.getOrElse(Seq.empty).size,
          totalTopic = courseInfo.deckIds.getOrElse(Seq.empty).size,
          completedCard = completedCardCount,
          learningCard = 0,
          totalCard = courseInfo.totalCard.getOrElse(0),
          level = courseInfo.level,
          name = courseInfo.name,
          thumbnail = courseInfo.thumbnail,
          description = courseInfo.description,
          creatorId = courseInfo.creator
        )
      })
    } yield {
      PageResult[LearningCourseInfo](
        courseList.size,
        learningCourses.sortBy(_.completedCard)(Ordering[Int].reverse)
      )
    }
  }

  override def getAvailableCourses(from: Int, size: Int): Future[PageResult[CourseInfo]] = {
    search(SearchRequest(
      from = from,
      size = size,
      terms = Some(Seq(TermsQuery("status", ListBuffer(TextNode.valueOf("1")))))
    ))
  }


  private def validatePerms(user: SignedInUser, course: CourseInfo) = {
    course.creator match {
      case Some(owner) if !owner.equals(user.username) =>
        throw UnAuthorizedError(Some(s"No permission to act on this course."))
      case _ => true
    }
  }

  override def getCardIds(courseInfo: CourseInfo): Future[Seq[String]] = {
    for {
      deckIds <- Future.value(courseInfo.deckIds.getOrElse(ListBuffer.empty))
      deckMap <- deckRepository.multiGet(deckIds)
      cardIds = deckIds.map(deckMap.get(_))
        .filter(_.isDefined)
        .map(_.get)
        .map(_.cards.getOrElse(ListBuffer.empty))
        .flatten
    } yield {
      cardIds
    }
  }
}




