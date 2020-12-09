package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import xed.api_service.domain.request._
import xed.api_service.service.SRSService
import xed.api_service.service.statistic.StatisticService
import xed.chatbot.domain.leaderboard.LeaderBoardItem
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

  get(s"$apiPath/top/by_new_card") {
    request: GetTopByNewCardRequest =>
      Profiler(s"$clazz.getTopByNewCard") {
        statisticService.getTopByNewCard(request).flatMap(injectOwnerDetails(_))
      }
  }


  private def injectOwnerDetails(pageResult: Seq[LeaderBoardItem]): Future[Seq[LeaderBoardItem]] = {

    val userNames = pageResult.map(_.username)

    val injectFn = (requests: Seq[LeaderBoardItem], users: Map[String, ShortUserProfile]) => {
      requests.map(item => item.copy(
        userProfile = users.get(item.username)
      ))
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(pageResult, shortProfiles)
    } yield {
      r
    }
  }

}
