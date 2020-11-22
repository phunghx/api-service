package xed.api_service.service.bq

import com.google.inject.Inject
import com.google.inject.name.Named
import com.twitter.util.{Future, FuturePool}
import com.typesafe.config.Config
import scalaj.http.{Http, HttpOptions}
import xed.api_service.util.{JsonUtils, LoggerUtils}
import xed.userprofile.domain.UserProfile
/**
 * Created by phg on 2020-01-28.
 **/

case class BatchEventLog(records: Array[EventLog])

case class EventLog(
                     eventName: String,
                     timestamp: Option[Long] = None,
                     source: String,
                     eventParams: Option[Map[String, Any]] = None,
                     userId: String,
                     userPseudoId: Option[String] = None,
                     userProperties: Option[UserProfile] = None
                   )

trait LogService {
  def log(event: EventLog): Future[Boolean]

  def log(events: Array[EventLog]): Future[Boolean]
}

case class HttpLogService @Inject()(@Named("remote-logger-config") config: Config) extends LogService {

  private val logger = LoggerUtils.getLogger("RemoteLogger")

  private val logUrl = config.getString("remote-url")

  private val futurePool = FuturePool.unboundedPool

  override def log(event: EventLog):  Future[Boolean] = futurePool {
    val res = Http(s"$logUrl/events").header("Content-Type", "application/json;charset=utf-8")
      .timeout(60000, 60000)
      .postData(JsonUtils.toJson(event, isPretty = false))
      .option(HttpOptions.followRedirects(true))
      .asString
    if (res.isError) logger.info(res.body)
    res.isSuccess
  }

  override def log(events: Array[EventLog]):  Future[Boolean] = futurePool {
    val res = Http(s"$logUrl/events").header("Content-Type", "application/json;charset=utf-8")
      .timeout(60000, 60000)
      .postData(JsonUtils.toJson(BatchEventLog(events), isPretty = false))
      .option(HttpOptions.followRedirects(true))
      .asString
    if (res.isError) logger.info(res.body)
    res.isSuccess
  }
}

