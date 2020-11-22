package xed.userprofile

import com.twitter.util.Future
import xed.userprofile.domain.UserProfile
import xed.userprofile.domain.UserProfileImplicits.TUserProfileWrapper
import xed.userprofile.service.TUserProfileService

trait UserProfileService  {
  def getProfile(username: String): Future[Option[UserProfile]]

  def getProfileByEmail(email: String): Future[Option[UserProfile]]

  def getProfiles(usernames: Seq[String]): Future[Map[String,UserProfile]]
}


case class UserProfileServiceTestImpl() extends UserProfileService {

  override def getProfile(username: String): Future[Option[UserProfile]] = {
    Future.value(Some(UserProfile(
      username = username,
      firstName = Some("XED"),
      lastName = None,
      fullName = Some("XED Content"),
      email = None,
      avatar = Some("https://www.picclickimg.com/d/l400/pict/182530233969_/Dragon-Ball-Z-Super-Prince-Vegeta-Saiyan-God.jpg"),
      mobilePhone = None,
      additionalInfo = None
    )))
  }

  override def getProfileByEmail(email: String): Future[Option[UserProfile]] = {
    Future.value(Some(UserProfile(
      username = "up-be198039-0170-4da5-a17f-50a1c05c4d67",
      firstName = Some("XED"),
      lastName = None,
      fullName = Some("XED Content"),
      email = None,
      avatar = Some("https://www.picclickimg.com/d/l400/pict/182530233969_/Dragon-Ball-Z-Super-Prince-Vegeta-Saiyan-God.jpg"),
      mobilePhone = None,
      additionalInfo = None
    )))
  }

  override def getProfiles(usernameList: Seq[String]): Future[Map[String,UserProfile]] = {
    Future{
      usernameList.zipWithIndex.map(e => {
        UserProfile(
          username = e._1,
          firstName = Some(s"Vegeta ${e._2}"),
          lastName = Some("Prince"),
          fullName = Some(s"Prince Vegeta ${e._2}"),
          email = None,
          avatar = Some("https://www.picclickimg.com/d/l400/pict/182530233969_/Dragon-Ball-Z-Super-Prince-Vegeta-Saiyan-God.jpg"),
          mobilePhone = None,
          additionalInfo = None
        )
      }).map(e => e.username -> e).toMap
    }
  }

}


case class UserProfileServiceImpl(client: TUserProfileService.MethodPerEndpoint) extends UserProfileService {


  override def getProfile(username: String): Future[Option[UserProfile]] = {
    for {
      r <- client.getUserProfile(username)
    } yield r.exist match {
      case true => r.userProfile.map(_.toUserProfile)
      case _ => None
    }
  }

  override def getProfileByEmail(email: String): Future[Option[UserProfile]] = {
    client.getProfileByEmail(email).map({
      case response if response.exist =>
        response.userProfile.map(_.toUserProfile)
      case _ => None
    })

  }

  override def getProfiles(usernameList: Seq[String]): Future[Map[String,UserProfile]] = {
    for {
      r <- client.multiGetUserProfiles(usernameList.toSet)
    } yield r.total> 0 match {
      case true =>
        r.userProfiles
          .map(_.map(e => (e._1 -> e._2.toUserProfile)).toMap)
          .getOrElse(Map.empty)
      case _ => Map.empty
    }
  }

}
