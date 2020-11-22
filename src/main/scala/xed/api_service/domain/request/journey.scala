package xed.api_service.domain.request

import java.util.UUID

import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{MethodValidation, NotEmpty, ValidationResult}
import xed.api_service.domain.Status
import xed.api_service.domain.course.{CategoryInfo, CourseInfo, JourneyInfo}
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer

case class CreateXCategoryRequest(name: String,
                                 documentStatus: Option[Int]) {
  @MethodValidation
  def validateRequest() = {
    val status = documentStatus.getOrElse(Status.PUBLISHED.id)

    ValidationResult.validate(Status.isValid(status),s"[document_status] is invalid.")
  }

  def build() = {
    CategoryInfo(
      id = UUID.randomUUID().toString,
      name = Some(name),
      documentStatus =Some( documentStatus.getOrElse(Status.PUBLISHED.id)),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}

case class UpdateXCategoryRequest(@RouteParam id : String,
                                 name: Option[String],
                                 documentStatus: Option[Int])

case class CreateJourneyRequest(@NotEmpty name: String,
                                thumbnail: Option[String],
                                description: Option[String],
                                decks: Option[Seq[String]]) {

  def build(user: SignedInUser) = {

    JourneyInfo(id = UUID.randomUUID().toString,
      name = Some(name),
      thumbnail = thumbnail,
      description = description,
      creator = Some(user.username),
      status = Some(Status.PROTECTED.id),
      deckIds = decks.flatMap(x => if(x.nonEmpty) Some(ListBuffer(x:_*)) else None),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}


case class UpdateJourneyRequest(@RouteParam @NotEmpty journeyId: String,
                                name: Option[String],
                                thumbnail: Option[String],
                                description: Option[String],
                                documentStatus: Option[Int],
                                deckIds: Option[ListBuffer[String]]) {


  def build(user: SignedInUser) = {
    JourneyInfo(id = journeyId,
      name = name.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      thumbnail = thumbnail.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      description = description.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      creator = Some(user.username),
      status = documentStatus,
      deckIds = deckIds,
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = None
    )
  }
}


case class ListAllCategoryRequest(@QueryParam status: Option[Int])


case class PublishJourneyRequest(@RouteParam @NotEmpty journeyId: String,
                                 status: Option[Int]) {
  @MethodValidation
  def validateRequest() = {
    val x = status.getOrElse(Status.PUBLISHED.id)

    ValidationResult.validate(Status.PUBLISHED.id == x,s"[status] must be a release status.")
  }
}

case class SkipCardRequest(@RouteParam @NotEmpty journeyId: String,
                           @NotEmpty cardIds: Seq[String])




case class CreateCourseRequest(@NotEmpty name: String,
                               @NotEmpty level: String,
                               thumbnail: Option[String],
                               description: Option[String],
                               journeyIds: Option[Seq[String]]) {

  def build(user: SignedInUser) = {

    CourseInfo(
      id = UUID.randomUUID().toString,
      level = Some(level),
      name = Some(name),
      thumbnail = thumbnail,
      description = description,
      creator = Some(user.username),
      status = Some(Status.PROTECTED.id),
      journeyIds = journeyIds.flatMap(x => if(x.nonEmpty) Some(ListBuffer(x:_*)) else None),
      deckIds = None,
      totalCard = None,
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}


case class UpdateCourseRequest(@RouteParam @NotEmpty id: String,
                               level: Option[String],
                               name: Option[String],
                               thumbnail: Option[String],
                               description: Option[String],
                               status: Option[Int],
                               journeyIds: Option[ListBuffer[String]]) {


  def build(user: SignedInUser) = {

    CourseInfo(id = id,
      level = level.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      name = name.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      thumbnail = thumbnail.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      description = description.flatMap(x => if (x == null || x.isEmpty) None else Some(x)),
      creator = Some(user.username),
      status = status,
      journeyIds = journeyIds,
      deckIds = None,
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = None,
      totalCard = None
    )
  }
}