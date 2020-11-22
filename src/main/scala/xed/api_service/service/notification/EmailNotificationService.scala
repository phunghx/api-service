package xed.api_service.service.notification

import akka.actor.{Actor, ActorLogging}
import xed.api_service.util.mail._


case class EmailData(fullName: String,
                     exportName: String,
                     time: String,
                     link: Option[String],
                     detailError: Option[String])

case class BugData(title: String,
                   fullName: String,
                   firstName: String,
                   time: String,
                   reqBody: Option[String],
                   respBody: Option[String],
                   deviceInfo: Option[String])

class EmailNotificationService(mailService: EmailService,
                               exportSuccessMailFormatter: MailFormatter,
                               exportFailureMailFormatter: MailFormatter,
                               bugReportMailFormatter: MailFormatter) extends Actor with ActorLogging {

  override def receive: Receive = {

    case _ =>
  }
}
