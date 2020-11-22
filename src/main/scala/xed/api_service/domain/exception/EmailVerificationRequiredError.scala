package xed.api_service.domain.exception

import com.twitter.finagle.http.Status

/**
 * @author anhlt
 */
case class EmailVerificationRequiredError(message:  Option[String] = None, cause: Throwable = null)
  extends XedException(XedException.EmailVerificationRequired,message, cause){
  override def getStatus: Status = Status.InternalServerError
}
