package xed.api_service.domain.exception

case class NotFoundError(message: Option[String] = None, cause: Throwable = null) extends XedException(XedException.NotFound, message,cause) {
  override def getStatus = com.twitter.finagle.http.Status.NotFound
}