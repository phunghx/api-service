package xed.api_service.domain

object Status extends Enumeration {
  type Status = Status.Value
  val PROTECTED = Value(0)
  val PUBLISHED = Value(1)
  val DELETED = Value(2)

  def isValid(x: Int) = {
    Status.values
      .filter(_.id == x)
      .nonEmpty
  }
}