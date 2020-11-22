package xed.api_service.domain.request

import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{MethodValidation, NotEmpty, ValidationResult}


case class GetTopByNewCardRequest(@QueryParam fromTime: Long,
                                  @QueryParam toTime: Long)

case class GetReportRequest(@QueryParam interval: Int,
                            @QueryParam fromTime: Long,
                            @QueryParam toTime: Long)



case class LikeRequest(@RouteParam @NotEmpty objectType: String,
                       @RouteParam @NotEmpty objectId: String) {

  @MethodValidation
  def validate() = {
    objectType match {
      case "deck" => ValidationResult.Valid
      case _ => ValidationResult.Invalid(s"Unsupport object_type: $objectType")
    }
  }

}

case class CheckVoteStatusRequest(@RouteParam @NotEmpty objectType: String,
                           @NotEmpty objectIds: Seq[String])

case class GetVotesRequest(@RouteParam @NotEmpty objectType: String,
                           @NotEmpty objectIds: Seq[String])

