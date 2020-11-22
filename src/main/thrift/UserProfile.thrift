#@namespace scala xed.userprofile.service
include "UserProfileDT.thrift"

service TUserProfileService {

    string ping()

    UserProfileDT.TFullUserAuthInfoResp checkSession(
            1: required string sessionId
    )

    UserProfileDT.TFullUserInfoResp getUserProfileBySessionID(
        1: required string sessionId
    )

    UserProfileDT.TFullUserInfoResp getUserProfileByUsername(
        1: required string username
    )

    UserProfileDT.TUserProfileResp getUserProfile(
        1: required string username
    )

    UserProfileDT.TUserProfileResp getProfileByEmail(
        1: required string email
    )

    UserProfileDT.TMultiUserProfileResp multiGetUserProfiles(
        1: required set<string> usernames
    )

    UserProfileDT.TUserProfileSearchResp getUserProfiles(
            1: required i32 from
            2: required i32 size
    )
}

