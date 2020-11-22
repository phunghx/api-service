package xed.api_service.domain.exception

case class BadRequestError(message: Option[String] = None, cause: Throwable = null)
  extends XedException(XedException.BadRequest, message,cause ) {
  override def getStatus = com.twitter.finagle.http.Status.BadRequest
}