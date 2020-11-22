package xed.api_service.domain.response

case class PageResult[+T](total: Long, records: Seq[T])