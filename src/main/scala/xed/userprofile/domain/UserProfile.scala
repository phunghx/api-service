package xed.userprofile.domain

import xed.userprofile.domain.thrift.TUserProfile

case class UserProfile(username: String,
                       firstName: Option[String] = None,
                       lastName: Option[String] = None,
                       fullName: Option[String] = None,
                       email: Option[String] = None,
                       avatar:Option[String] = None,
                       mobilePhone: Option[String] = None,
                       nationality: Option[String] = None,
                       nativeLanguages: Option[Seq[String]] = None,
                       additionalInfo: Option[Map[String,String]] = None
                      )

case class ShortUserProfile(username: String,
                            firstName: Option[String],
                            lastName: Option[String],
                            fullName: Option[String],
                            avatar:Option[String])


object UserProfileImplicits {

  implicit class TUserProfileWrapper(profile: TUserProfile) {

    def toUserProfile = UserProfile(
      username = profile.username,
      firstName = profile.firstName,
      lastName = profile.lastName,
      fullName = profile.fullName,
      email = profile.email,
      avatar = profile.avatar,
      mobilePhone = profile.mobilePhone,
      nationality = profile.nationality,
      nativeLanguages = profile.nativeLanguages,
      additionalInfo = profile.additionalInfo.map(_.toMap)
    )
  }



  implicit class UserProfileWrapper(profile: UserProfile) {
    def toShortProfile = ShortUserProfile(
      username = profile.username,
      firstName = profile.firstName,
      lastName = profile.lastName,
      fullName = profile.fullName,
      avatar = profile.avatar
    )
  }


}
