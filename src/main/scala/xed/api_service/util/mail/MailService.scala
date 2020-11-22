package xed.api_service.util.mail

import java.util.Properties

import com.google.inject.Inject
import com.twitter.inject.Logging
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import xed.api_service.util.JsonUtils


trait EmailService {
  def sendHtmlMessage(from: String,
                      recipient: String,
                      subject: String,
                      data: Any,
                      formatter: MailFormatter): Unit

  def sendHtmlMessage(from: String,
                      recipient: String,
                      bccRecipient: Option[String],
                      ccRecipient: Option[String],
                      subject: String,
                      data: Any,
                      formatter: MailFormatter): Unit
}

class FakeEmailService() extends EmailService with Logging {
  override def sendHtmlMessage(from: String, recipient: String, subject: String, data: Any, formatter: MailFormatter): Unit = {
    info(s"Email send: $from $recipient ${JsonUtils.toJson(data)}")
  }

  override def sendHtmlMessage(from: String, recipient: String, bccRecipient: Option[String], ccRecipient: Option[String], subject: String, data: Any, formatter: MailFormatter): Unit = {
    info(s"Email send: $from $recipient $ccRecipient $bccRecipient ${JsonUtils.toJson(data)}")
  }
}

class SMTPEmailService @Inject()(config: MailServerConfig) extends EmailService {

  private val properties = {
    val props = new Properties()
    props.put("mail.smtp.host", config.host)
    props.put("mail.smtp.port", config.port.toString)
    props.put("mail.smtp.auth", config.auth.toString)
    props.put("mail.smtp.starttls.enable", config.tls.toString)
    if (config.tls) {
      props.put("mail.smtp.socketFactory.class", config.factoryClass.get)
    }
    props
  }
  private val authenticator = if (config.auth) {
    if (config.username.isEmpty || config.password.isEmpty) {
      throw new Exception("username and password are required when auth is enabled")
    }
    new Authenticator() {
      override protected def getPasswordAuthentication = new PasswordAuthentication(config.username.get, config.password.get)
    }
  } else null

  private val session: Session =
    Session.getInstance(properties, authenticator)

  def sendHtmlMessage(
                       from: Option[Address],
                       recipients: Array[Address],
                       ccRecipients: Array[Address],
                       bccRecipients: Array[Address],
                       subject: String,
                       body: String
                     ): Unit = {
    val message = new MimeMessage(session)
    if (from.isDefined) message.setFrom(from.get)
    message.setRecipients(Message.RecipientType.TO, recipients)
    if (ccRecipients.nonEmpty) message.setRecipients(Message.RecipientType.CC, ccRecipients)
    if (bccRecipients.nonEmpty) message.setRecipients(Message.RecipientType.BCC, bccRecipients)
    message.setSubject(subject)
    message.setText(body, "utf-8", "html")
    import javax.mail.Transport
    Transport.send(message)
  }

  def sendHtmlMessage(from: String, recipient: String, subject: String, body: String): Unit = {
    sendHtmlMessage(
      Some(new InternetAddress(from)),
      InternetAddress.parse(recipient).toArray[Address],
      Array.empty,
      Array.empty,
      subject,
      body
    )
  }

  override def sendHtmlMessage(from: String, recipient: String, subject: String, data: Any, formatter: MailFormatter): Unit = {
    sendHtmlMessage(from, recipient, subject, formatter.format(data))
  }

  override def sendHtmlMessage(from: String, recipient: String, bccRecipient: Option[String], ccRecipient: Option[String], subject: String, data: Any, formatter: MailFormatter): Unit = {
    sendHtmlMessage(
      Some(new InternetAddress(from)),
      InternetAddress.parse(recipient).toArray[Address],
      ccRecipient.map(a => InternetAddress.parse(a).toArray[Address]).getOrElse(Array.empty),
      bccRecipient.map(a => InternetAddress.parse(a).toArray[Address]).getOrElse(Array.empty),
      subject,
      formatter.format(data)
    )
  }

}

