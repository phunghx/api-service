package xed.userprofile

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Local
import xed.api_service.domain.exception.UnAuthenticatedError
import xed.userprofile.domain.UserProfile


/**
 * @author anhlt
 */
case class SignedInUser(
  session: String,
  username: String,
  userProfile: Option[UserProfile] = None,
  roles: Set[Int] = Set.empty[Int],
  var permissions: Set[String] = Set.empty[String],
  var permissionsWithRole: Map[Int, Set[String]] = Map.empty[Int, Set[String]])

case class AuthInfo(user: Option[SignedInUser], performer: Option[PerformerInfo])

case class PerformerInfo(source: String, performer: Option[String])

trait SessionHolder {
  def optUser: Option[SignedInUser]

  def getUser: SignedInUser

  def optPerformerInfo: Option[PerformerInfo]

  def optPerformer: Option[String] = optPerformerInfo.flatMap(_.performer)

  def optSource: Option[String] = optPerformerInfo.map(_.source)

  def setAuth[B](user: Option[SignedInUser], performer: Option[PerformerInfo])(fn: => B): B

  def getPermissions: Map[Int, Set[String]]

  def setPermissions(permissions: Map[Int, Set[String]]): Unit
}

@Singleton
case class SessionHolderImpl @Inject()(authenService: AuthenService) extends SessionHolder {
  val local = new Local[Option[AuthInfo]]()

  override def setAuth[B](user: Option[SignedInUser], performer: Option[PerformerInfo])(fn: => B): B = {
    local.let(Some(AuthInfo(user, performer)))(fn)
  }

  override def optPerformerInfo: Option[PerformerInfo] = local().flatten.flatMap(_.performer)

  override def getPermissions = getUser.permissionsWithRole

  override def setPermissions(permissions: Map[Int, Set[String]]): Unit = {
    getUser.permissionsWithRole = permissions
  }

  override def getUser: SignedInUser = optUser.getOrElse(throw UnAuthenticatedError())

  override def optUser: Option[SignedInUser] = local().flatten.flatMap(_.user)
}


