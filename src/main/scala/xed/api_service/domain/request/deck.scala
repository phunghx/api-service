package xed.api_service.domain.request

import java.util.UUID

import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.NotEmpty
import xed.api_service.domain._
import xed.api_service.domain.design.Container
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer


case class GetDeckRequest(@NotEmpty deckIds: Seq[String])

case class CreateDeckRequest(@NotEmpty name: String,
                             description: Option[String],
                             thumbnail: Option[String],
                             category: Option[String],
                             deckStatus: Option[Int],
                             design: Option[Container]) {

  def build(user: SignedInUser) = {
    Deck(
      id = UUID.randomUUID().toString,
      username = Some(user.username),
      name = Some(name),
      description = description,
      thumbnail = thumbnail,
      category = category,
      design = design,
      cards = Some(ListBuffer.empty[String]),
      deckStatus = if(deckStatus.isDefined) deckStatus else Some(Status.PROTECTED.id),
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis()))
  }
}

case class EditDeckRequest(@RouteParam @NotEmpty deckId: String,
                           name: Option[String],
                           thumbnail: Option[String],
                           description: Option[String],
                           category: Option[String],
                           deckStatus: Option[Int],
                           design: Option[Container]) {

  def build(user: SignedInUser) = {
    val createdTime =  deckStatus match {
      case Some(status) if status ==  Status.PUBLISHED.id =>  Some(System.currentTimeMillis())
      case _ => None
    }

    Deck(
      id = deckId,
      username = None,
      name = name.flatMap(x => if(x== null || x.isEmpty) None else Some(x)),
      thumbnail = thumbnail.flatMap(x => if(x== null || x.isEmpty) None else Some(x)),
      description = description.flatMap(x => if(x== null || x.isEmpty) None else Some(x)),
      category = category.flatMap(x => if(x == null) None else Some(x)).map(_.trim),
      design = design,
      cards = None,
      deckStatus = deckStatus,
      updatedTime = Some(System.currentTimeMillis()),
      createdTime = createdTime)

  }
}

case class ChangeLikeRequest(@QueryParam sk: String,
                             totalLikes: Option[Map[String, Int]]) {

  def build() : Seq[Deck] = {
    val deckMap = scala.collection.mutable.Map.empty[String,Deck]
     totalLikes.getOrElse(Map.empty)
      .filter(_._2 >=0)
       .map({
      case (deckId, totalLikes) =>
        deckMap.put(deckId, Deck(
          id = deckId,
          username = None,
          name = None,
          thumbnail =None,
          description = None,
          category = None,
          design = None,
          cards = None,
          totalLikes = Option(totalLikes),
          updatedTime = Some(System.currentTimeMillis())
        ))
    })

    deckMap.values.toSeq
  }
}

case class ChangeWeekyLikeRequest(@QueryParam sk: String,
                                  totalLikes: Option[Map[String, Int]]) {

  def build() : Seq[Deck] = {
    val deckMap = scala.collection.mutable.Map.empty[String, Deck]
    totalLikes.getOrElse(Map.empty)
      .filter(_._2 >= 0)
      .map({
        case (deckId, weeklyTotalLikes) =>
          deckMap.put(deckId, Deck(
            id = deckId,
            username = None,
            name = None,
            thumbnail = None,
            description = None,
            category = None,
            design = None,
            cards = None,
            weeklyTotalLikes = Option(weeklyTotalLikes),
            updatedTime = Some(System.currentTimeMillis())
          ))
      })

    deckMap.values.toSeq
  }
}
