package xed.api_service.service.statistic

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.domain.Deck
import xed.api_service.domain.metric.{CardReport, LineData}
import xed.api_service.domain.request.{GetReportRequest, GetTopByNewCardRequest}
import xed.api_service.repository.{ReviewHistoryRepository, SRSRepository}
import xed.api_service.service.SRSService
import xed.api_service.util.Implicits.FutureEnhance
import xed.api_service.util.{Implicits, TimeUtils, ZConfig}
import xed.userprofile.SignedInUser

trait StatisticService {

  def getCardReport(user: SignedInUser, request: GetReportRequest): Future[Map[String, LineData]]

  def getCardReportV2(user: SignedInUser, request: GetReportRequest): Future[CardReport]


  def getReviewTimeChart(user: SignedInUser, request: GetReportRequest): Future[LineData]


}


case class StatisticServiceImpl@Inject()(srsService: SRSService,
                                         reviewHistoryRepository: ReviewHistoryRepository) extends StatisticService {

  private val cacheIntervalInMinutes = ZConfig.getInt("cache.top_learners.interval_in_mins", 5)
  private val cacheSize = ZConfig.getInt("cache.top_learners.size", 500)


  override def getCardReport(user: SignedInUser, request: GetReportRequest): Future[Map[String, LineData]] = {
    srsService.getCardReport(user, request)
  }

  override def getCardReportV2(user: SignedInUser, request: GetReportRequest): Future[CardReport] = {
    srsService.getCardReportV2(user, request)
  }


  override def getReviewTimeChart(user: SignedInUser, request: GetReportRequest) = {
    reviewHistoryRepository.getReviewTimeChart(
      user.username,
      request.interval,
      request.fromTime,
      request.toTime
    )
  }

}