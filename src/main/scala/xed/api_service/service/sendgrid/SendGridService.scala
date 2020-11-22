package xed.api_service.service.sendgrid

import com.google.inject.Inject
import com.google.inject.name.Named
import com.sendgrid._
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Content, Email, Personalization}
import com.twitter.inject.Logging
import com.typesafe.config.Config

/**
 * Created by phg on 2020-03-12.
 **/
trait SendGridService {
  def sendTemplate(templateId:String, subject: String, to: Seq[Personalization])
}

case class SendGridServiceImpl @Inject()(
  @Named("sendgrid-config") config: Config
) extends SendGridService with Logging {

  private val _apiKey = config.getString("api-key")
  private val _from = new Email(config.getString("from.email"), config.getString("from.name"))

  private def _sendTemplate(templateId: String, subject: String, to: Seq[Personalization]): Response = {
    val mail = new Mail()
    mail.setTemplateId(templateId)
    mail.setFrom(_from)
    mail.setSubject(subject)
    mail.addContent(new Content(
      "text/html", "1"
    ))

    to.foreach(mail.addPersonalization)

    val req = new Request
    req.setEndpoint("mail/send")
    req.setMethod(Method.POST)
    req.setBody(mail.build())

    val sg = new SendGrid(_apiKey)
    val res = sg.api(req)
    if (res.getStatusCode >= 400) {
      error(res.getBody)
    }
    res
  }

  override def sendTemplate(templateId: String, subject: String, to: Seq[Personalization]): Unit = {
    _sendTemplate(templateId, subject, to)
  }
}