package xed.userprofile

import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.domain.exception.InternalError
import xed.caas.domain.thrift.{TRoleInfo, TUserInfo}
import xed.caas.service.TCaasService

/**
 * Created by anhlt
 */
trait AuthenService {
  def getUserWithSessionId(sessionId: String): Future[Option[TUserInfo]]

  def hasRole(sessionId: String, roleName: String): Future[Option[Boolean]]

  def hasRoles(sessionId: String, roleNames: Seq[String]): Future[Option[Boolean]]

  def hasRoleUser(username: String, roleName: String): Future[Option[Boolean]]

  def hasAllRoleUser(username: String, roleNameS: Seq[String]): Future[Option[Boolean]]

  def insertUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]]

  def deleteUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]]

  def getUserRoles(username: String): Future[Option[Seq[TRoleInfo]]]

  def getAllPermission(username: String): Future[Option[Seq[String]]]

  def deleteAllExpiredUserRole(defaultRole: Int, maxTime: Long): Future[Option[Boolean]]

}

case class AuthenServiceImpl @Inject()(client: TCaasService.MethodPerEndpoint) extends AuthenService with Logging {

  def getUserWithSessionId(sessionId: String): Future[Option[TUserInfo]] = {
    for {
      r <- client.getUserWithSessionId(sessionId)
    } yield r.code match {
      case 0 => r.userInfo
      case _ =>
        error(s"Error in getUserWithSessionId: $sessionId - ${r.msg.getOrElse("")}")
        None
    }
  }

  override def getUserRoles(username: String): Future[Option[Seq[TRoleInfo]]] = {
    client.getAllRoleInfo(username).map(r =>{
      r.code match {
        case 0 =>
          val currentTime = System.currentTimeMillis()
          r.roles.map(_.filter(_.expireTime.getOrElse(Long.MaxValue) > currentTime))
        case _ => None
      }
    })

  }

  override def hasRole(sessionId: String, roleName: String): Future[Option[Boolean]] = {
    for {
      r <- client.hasRole(sessionId, roleName)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def hasRoles(sessionId: String, roleNames: Seq[String]): Future[Option[Boolean]] = {
    for {
      r <- client.hasRoles(sessionId, roleNames)
    } yield r.code match {
      case 0 => r.data.map(!_.exists(_ == false))
      case _ => None
    }
  }

  override def hasRoleUser(username: String, roleName: String): Future[Option[Boolean]] = {
    for {
      r <- client.hasRoleUser(username, roleName)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def hasAllRoleUser(username: String, roleNames: Seq[String]): Future[Option[Boolean]] = {
    for {
      r <- client.hasAllRoleUser(username, roleNames)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def insertUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]] = {
    for {
      r <- client.insertUserRoles(username, roleIds)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def deleteUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]] = {
    for {
      r <- client.deleteUserRoles(username, roleIds)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def getAllPermission(username: String): Future[Option[Seq[String]]] = {
    for {
      r <- client.getAllPermission(username)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }

  override def deleteAllExpiredUserRole(defaultRole: Int, maxTime: Long): Future[Option[Boolean]] = {
    for {
      r <- client.deleteAllExpiredUserRole(defaultRole, maxTime)
    } yield r.code match {
      case 0 => r.data
      case _ => None
    }
  }
}

case class AuthenServiceTestImpl() extends AuthenService with Logging {

  def getUserWithSessionId(sessionId: String): Future[Option[TUserInfo]] = Future {
    val x = TUserInfo(
      username = "up-be198039-0170-4da5-a17f-50a1c05c4d67",
      isActive = true,
      createTime = System.currentTimeMillis(),
      roles = Nil
    )
    Some(x)
  }

  override def getUserRoles(username: String): Future[Option[Seq[TRoleInfo]]] = Future {
    Some(
      Seq(
        TRoleInfo(11, "vip", Set.empty, Some(Long.MaxValue))
      )
    )
  }


  override def hasRole(sessionId: String, roleName: String): Future[Option[Boolean]] = Future.None

  override def hasRoles(sessionId: String, roleNames: Seq[String]): Future[Option[Boolean]] = Future.None

  override def hasRoleUser(username: String, roleName: String): Future[Option[Boolean]] = Future.None

  override def hasAllRoleUser(username: String, roleNameS: Seq[String]): Future[Option[Boolean]] = Future.None

  override def insertUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]] = Future.None

  override def deleteUserRoles(username: String, roleIds: Set[Int]): Future[Option[Boolean]] = Future.None

  override def getAllPermission(username: String): Future[Option[Seq[String]]] = Future.None

  override def deleteAllExpiredUserRole(defaultRole: Int, maxTime: Long): Future[Option[Boolean]] = Future.None
}
