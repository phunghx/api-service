#@namespace scala xed.caas.domain.thrift
const string OAUTH_GOOGLE = "google"
const string OAUTH_FACEBOOK = "facebook"
const string OAUTH_U_P = "u_p"
struct TRoleInfo {
    1:required i32 id
    2:required string name
    3:required set<string> permissions
    4:optional i64 expireTime
}
struct TUserInfo {
    1:required string username
    2:required bool isActive
    3:required i64 createTime
    4:required list<TRoleInfo> roles
}
struct TUserInfoPageable{
    1:required i32 code
    2:optional string msg
    3:optional list<TUserInfo> users
    4:optional i64 total
}
struct TUserAuthInfo {
    1:required string ssid
    2:required TUserInfo userInfo
}
struct TUserAuthResult{
    1:required i32 code
    2:optional string msg
    3:optional TUserAuthInfo userAuthInfo
}
struct TUserInfoResult{
    1:required i32 code
    2:optional string msg
    3:optional TUserInfo userInfo
}
struct TResult{
    1:required i32 code
    2:optional string msg
}
struct TBoolResult
{
  1:required i32 code
  2:optional string msg
  3:optional bool data
}
struct TListBoolResult
{
  1:required i32 code
  2:optional string msg
  3:optional list<bool> data
}
struct TListStringResult
{
  1:required i32 code
  2:optional string msg
  3:optional list<string> data
}
struct TStringResult
{
  1:required i32 code
  2:optional string msg
  3:optional string data
}
struct TListUserResult
{
    1:required i32 code
    2:optional i64 total
    3:optional list<string> users
    4:optional string msg
}
struct TListRoleInfoResult {
    1: required i32 code
    2: required string username
    3: optional string msg
    4: optional list<TRoleInfo> roles
}