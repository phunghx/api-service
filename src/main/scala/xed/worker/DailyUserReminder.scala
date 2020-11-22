package xed.worker

import java.util.{Calendar, TimeZone}

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import org.elasticsearch.client.transport.TransportClient
import xed.api_service.service.SRSService
import xed.api_service.util.Running
import xed.notification.{Notification, NotificationService, PushNotification}

import scala.language.postfixOps

/**
 * Created by phg on 2020-02-19.
 **/
@Singleton
case class DailyUserReminder @Inject()(
  timeZone: TimeZone,
  srsService: SRSService,
  notificationService: NotificationService,
  @Named("user-profile-config") userProfileConfig: Config,
  esClient: TransportClient,
  userProfileHelper: UserProfileHelper
) extends Running {

  def startWorker(): Unit = {
    runEveryDayForEachTimeZone(20, 0)(timeZone => {
      info(s"DailyUserReminder - ${timeZone.getRawOffset} - ${timeZone.getID}")
      val users = userProfileHelper.searchUsers(offset = Some(timeZone.getRawOffset), lastOpenBefore = Some(1 dayBeforeInMillis))

//      _send(users.map(_.userId))
    })

  }

  private def _send(userIds: Array[String]): Unit = if (userIds.nonEmpty) {
    // TODO: Update message by user

    notificationService.sendPushNotification(PushNotification(
      userIds = Option(userIds),
      notification = Notification(
        title = "XED - The Next Flashcard",
        body = "Learn now"
      )
    ))
  }
}

case class UserWithTime(userId: String, nationality: Option[String] = None) {
  private val timeZone = nationality.getOrElse("") match {
    case _ => TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
  }

  def currentHour: Int = {
    val calendar = Calendar.getInstance(timeZone)
    calendar.get(Calendar.HOUR_OF_DAY)
  }
}