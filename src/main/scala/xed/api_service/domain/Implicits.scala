package xed.api_service.domain

import xed.api_service.domain.response.PageResult

object Implicits {

  implicit class SeqWrapper[T](items: Seq[T]) {

    def toPageResult = PageResult[T](items.size,items)
  }


}
