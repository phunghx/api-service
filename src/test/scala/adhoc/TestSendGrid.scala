package adhoc

import com.sendgrid.helpers.mail.objects.{Email, Personalization}
import xed.api_service.service.sendgrid.{SendGridService, SendGridServiceImpl}
import xed.api_service.util.ZConfig

/**
 * Created by phg on 2020-03-12.
 **/
object TestSendGrid {
  def main(args: Array[String]): Unit = {

    val service: SendGridService = SendGridServiceImpl(ZConfig.getConf("sendgrid"))
    val p = new Personalization()
    p.addTo(new Email("lamtungphuongtv@gmail.com", "Phuong Yahoo"))
    p.setSubject("Hello, 1")
    p.addDynamicTemplateData("firstname", "FirstName")
    p.addDynamicTemplateData("lastname", "LastName")
    val personalizations = Seq(p)
    service.sendTemplate(
      templateId = "d-f0f8625d6b9541eba14d76947de11cef",
      subject = "Hello Subject",
      to = personalizations
    )
  }
}
