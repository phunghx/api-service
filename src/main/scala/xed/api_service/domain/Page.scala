package xed.api_service.domain

/**
 * Created by phg on 2019-09-19.
 **/
trait Pageable {
  def from: Int

  def to: Int

  def page: Int

  def size: Int

  def first: Pageable

  def next: Pageable

  def nextToken: Option[String]

  def previousOrFirst: Pageable

  def sorts: Array[String]
}

trait Page[T] {
  def totalElement: Long

  def totalPage: Int

  def content: Array[T]

  def nextToken: Option[String]

  def currentPage: Int

  def from: Int

  def size: Int

  def hasNext: Boolean

  def nextPage: Pageable
}

case class PageImpl[T](content: Array[T], pageable: Pageable, total: Long, nextToken: Option[String] = None) extends Page[T] {

  override def totalElement: Long = total

  override def totalPage: Int = math.ceil(total * 1.0f / pageable.size).toInt

  override def currentPage: Int = pageable.page

  override def from: Int = pageable.from

  override def size: Int = pageable.size

  override def hasNext: Boolean = totalPage > currentPage

  override def nextPage: Pageable = pageable.next
}

case class PageNumberRequest(page: Int, size: Int, sorts: Array[String] = Array.empty) extends Pageable {

  override def from: Int = if (page > 1) (page - 1) * size else 0

  override def to: Int = from + size

  override def first: Pageable = PageNumberRequest(1, size)

  override def next: Pageable = PageNumberRequest(page + 1, size)

  override def previousOrFirst: Pageable = if (page >= 2) PageNumberRequest(page - 1, size) else first

  override def nextToken: Option[String] = None
}

case class PageTokenRequest(nextToken: Option[String] = None, size: Int = -1) extends Pageable {
  override def from: Int = -1

  override def to: Int = -1

  override def page: Int = -1

  override def first: Pageable = null

  override def next: Pageable = null

  override def previousOrFirst: Pageable = null

  override def sorts: Array[String] = null
}

object DefaultPageRequest extends PageNumberRequest(1, 10)