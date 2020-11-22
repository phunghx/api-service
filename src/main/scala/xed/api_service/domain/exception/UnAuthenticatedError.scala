package xed.api_service.domain.exception

case class UnAuthenticatedError(message: Option[String] = None, cause: Throwable = null) extends XedException(XedException.NotAuthenticated, message,cause) {
  override def getStatus = com.twitter.finagle.http.Status.Unauthorized
}