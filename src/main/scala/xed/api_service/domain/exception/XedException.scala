package xed.api_service.domain.exception

import com.twitter.finagle.http.Status

/**
  * @author anhlt
  */

object XedException {
  val InvalidCredentials = "invalid_credentials"
  val EmailExisted = "email_existed"
  val EmailNotExisted = "email_not_existed"
  val EmailInvalid = "email_invalid"
  val EmailRequired = "email_required"

  val EmailVerificationRequired = "email_verification_required"
  val AuthTypeUnsupported = "auth_type_unsupported"
  val PhoneExisted = "phone_existed"
  val PhoneInvalid = "phone_invalid"
  val PhoneNotExisted = "phone_not_existed"
  val PhoneRequired = "phone_required"
  val QuotaExceed = "quota_exceed"
  val RegistrationRequired = "registration_required"
  val VerificationCodeInvalid = "verification_code_invalid"


  val NotAuthenticated = "not_authenticated"
  val Unauthorized = "not_allowed"
  val NotFound = "not_found"
  val AlreadyExisted = "already_existed"
  val NotSupported = "not_supported"
  val SqlExecuteFailure = "db_execute_error"
  val Expired = "expired"
  val BadRequest = "bad_request"
  val InternalError = "internal_error"

}

abstract class XedException(val reason: String, message: Option[String] = None, cause: Throwable = null) extends Exception(message.getOrElse("Internal error"),cause) {

  def getStatus: Status

  override def getMessage: String = {
    if(message.nonEmpty) super.getMessage
    else if(cause!=null) cause.getMessage
    else message.getOrElse("Internal error")
  }
}


















