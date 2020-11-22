package xed.api_service.util.mail

case class EmailSendConfig(sender: String,
                           subject: String,
                           defaultSentAddress: String,
                           formatter: MailFormatter)

case class MailServerConfig(host: String,
                            port: Int,
                            auth: Boolean,
                            tls: Boolean,
                            username: Option[String] = None,
                            password: Option[String] = None,
                            factoryClass: Option[String] = Some("javax.net.ssl.SSLSocketFactory"),
                            fallback: Boolean = false)
