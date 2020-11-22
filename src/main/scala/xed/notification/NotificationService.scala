package xed.notification

import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config
import scalaj.http.Http
import xed.api_service.util.JsonUtils._
/**
 * Created by phg on 2020-02-19.
 **/
trait NotificationService {
  def sendPushNotification(notification: PushNotification)

  def sendEmailHtml(request: SendHtmlEmailRequest)

  def sendEmailTemplate(request: SendTemplateEmailRequest)
}

case class HttpNotificationService @Inject()(
  @Named("notification-config") config: Config
) extends NotificationService {

  private val host = config.getString("host")
  private val sk = config.getString("sk")

  override def sendPushNotification(notification: PushNotification): Unit = {
    Http(s"$host/in/messages?sk=$sk").postData(notification.toJsonString).asString
  }

  override def sendEmailHtml(request: SendHtmlEmailRequest): Unit = {
    Http(s"$host/in/emails/html?sk=$sk").postData(request.toJsonString).asString
  }

  override def sendEmailTemplate(request: SendTemplateEmailRequest): Unit = {
    Http(s"$host/in/emails/template?sk=$sk").postData(request.toJsonString).asString
  }
}

case class PushNotification(
  notification: Notification,
  data: Option[Map[String, String]] = None,
  userIds: Option[Array[String]] = None,
  topic: Option[String] = None,
  condition: Option[String] = None
)

case class Notification(
  title: String,
  body: String,
  imageUrl: Option[String] = None,
  aps: Option[ApplePush] = None
)

case class ApplePush(
  badge: Option[Int] = None
)

case class SendHtmlEmailRequest(
  category: String,
  emailDestination: EmailDestination,
  subject: String,
  html: String
)

case class SendTemplateEmailRequest(
  templateId: String,
  emailDestination: EmailDestination,
  subject: String,
  data: Map[String, Any]
)

case class EmailDestination(
  to: Array[String],
  cc: Array[String] = Array(),
  bcc: Array[String] = Array()
)

case class CreateTemplateResponse()