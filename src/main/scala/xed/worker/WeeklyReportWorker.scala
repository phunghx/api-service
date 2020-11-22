package xed.worker

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config
import xed.api_service.util.Running
import xed.notification.{EmailDestination, NotificationService, SendHtmlEmailRequest}

import scala.language.postfixOps

/**
 * Created by phg on 2020-03-12.
 **/
case class WeeklyReportWorker @Inject()(
  @Named("weekly-report-config") config: Config,
  userProfileHelper: UserProfileHelper,
  notificationService: NotificationService
) extends Running {

  private val dayOfWeek = config.getInt("day-of-week")
  private val hour = config.getInt("hour")
  private val minute = config.getInt("minute")

  private val templateId = config.getString("template-id")

  def startWorker(): Unit = {
    runEveryWeekForEachTimeZone(dayOfWeek, hour, minute)(timeZone => {
      info(s"WeeklyReportWorker - ${timeZone.getRawOffset} - ${timeZone.getID}")
      searchAndSendEmail(timeZone)
    })
  }

  private def searchAndSendEmail(timeZone: TimeZone): Unit = {
    val users = userProfileHelper.searchUsers(Some(timeZone.getRawOffset))
    if (users.nonEmpty) {
      info(s"WeeklyReportWorker ${users.map(_.userId).mkString(", ")}")
      val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      formatter.setTimeZone(timeZone)
      notificationService.sendEmailHtml(SendHtmlEmailRequest(
        category = "report",
        emailDestination = EmailDestination(
          to = Array("lamtungphuongtv@yahoo.com")
        ),
        subject = s"Send weekly email",
        html =
          s"""Send email weekly
             | Time: ${formatter.format(new Date())}
             | TO: ${users.map(_.email).mkString(", ")}
           """.stripMargin.replace("\n", "<br />")
      ))
    }
    // TODO: send email to users
//    notificationService.sendEmailTemplate(SendTemplateEmailRequest(
//      templateId = templateId,
//      emailDestination = EmailDestination(
//        to = Array()
//      ),
//      subject = "",
//      data = Map()
//    ))
  }
}

