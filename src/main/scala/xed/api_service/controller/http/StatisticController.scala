package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import xed.api_service.domain.request._
import xed.api_service.service.SRSService
import xed.api_service.service.statistic.StatisticService
import xed.profiler.Profiler
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class StatisticController @Inject()(statisticService: StatisticService,
                                         profileService: UserProfileService,
                                         sessionHolder: SessionHolder)  extends  Controller {
  private val clazz = getClass.getSimpleName
  private val apiPath = "/statistic"

  get(s"$apiPath/cards") {
    request: GetReportRequest =>
      Profiler(s"$clazz.getReportV1") {
        statisticService.getCardReport(
          sessionHolder.getUser,
          request
        )
      }
  }

  get(s"$apiPath/card_report") {
    request: GetReportRequest => Profiler(s"$clazz.getReportV2") {
        statisticService.getCardReportV2(sessionHolder.getUser, request)
      }
  }

  get(s"$apiPath/learning_time") {
    request: GetReportRequest =>
      Profiler(s"$clazz.getLearningTime") {
        statisticService.getReviewTimeChart(sessionHolder.getUser, request)
      }
  }
}
