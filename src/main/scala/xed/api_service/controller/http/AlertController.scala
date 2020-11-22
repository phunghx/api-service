package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import xed.api_service.domain.request._
import xed.api_service.service.AlertService
import xed.api_service.service.statistic.StatisticService
import xed.chatbot.domain.MarkMessageReadRequest
import xed.chatbot.domain.leaderboard.LeaderBoardItem
import xed.profiler.Profiler
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class AlertController @Inject()(alertService: AlertService,
                                     sessionHolder: SessionHolder)  extends  Controller {

  private val clazz = getClass.getSimpleName
  private val apiPath = "/alerts"

  get(s"$apiPath") {
    _: Request =>
      Profiler(s"$clazz.getAlerts") {
        alertService.getAlerts(
          sessionHolder.getUser
        )
      }
  }

}
