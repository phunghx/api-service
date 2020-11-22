#@namespace scala xed.userprofile.domain.thrift

struct TSessionInfo {
    1:required string key
    2:required string value
    3:required i64 timeoutInMs
    4:required string domain
    5:optional string path
}

struct TUserProfile {
    1:required string username
    2:optional string fullName
    3:optional string email
    4:optional string avatar
    5:optional string mobilePhone
    6:optional string lastName
    7:optional string firstName
    8:optional list<string> nativeLanguages
    9:optional string nationality
    10:optional map<string, string> additionalInfo
}

struct TUserInfo {
    1:required string username
    2:required bool isActive
    3:required i64 createTime
    4:required list<i32> roles
}

struct TFullUserInfoResp{
    1:required bool exist,
    2:optional TUserInfo userInfo,
    3:optional TUserProfile userProfile
}

struct TUserProfileResp{
    1: required bool exist,
    2: optional TUserProfile userProfile
}

struct TMultiUserProfileResp{
    1:required i64 total
    2: optional map<string,TUserProfile> userProfiles
}

struct TUserProfileSearchResp{
    1:required i64 total
    2:optional list<TUserProfile> users
}

struct TFullUserAuthInfoResp{
    1:required bool exist,
    2:optional TUserInfo userInfo,
    3:optional TUserProfile userProfile
    4:optional TSessionInfo session
}