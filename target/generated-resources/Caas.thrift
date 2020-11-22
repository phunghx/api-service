#@namespace scala xed.caas.service
include "CaasDT.thrift"
service TCaasService {
    string ping()
    CaasDT.TUserAuthResult renewSession(
        1: required string oldSessionId
        2: optional i64 sessionTimeout
    )
    CaasDT.TUserAuthResult login(
        1: required string username
        2: required string password
        3: optional i64 sessionTimeout
    )
    CaasDT.TUserAuthResult loginOAuth(
        1: required string username
        3: optional i64 sessionTimeout
    )
    CaasDT.TUserAuthResult loginWithOAuth(
        1: required string oauthType
        2: required string id
        3: required string token
        4: optional i64 sessionTimeout
        5: optional string password
    )
    CaasDT.TUserInfoResult register(
        1: required string username
        2: required string password
    )
    CaasDT.TBoolResult isCredentialDefault(
        1: required string oauthType
        2: required string username
    )
    CaasDT.TResult deleteUser(
        1: required string username
    )
    CaasDT.TUserInfoResult registerWithOAuth(
        1: required string oauthType
        2: required string id
        3: required string token
        4: optional string password
    )
    CaasDT.TUserInfoResult getUserWithSessionId(
        1: required string sessionId
    )
    CaasDT.TUserInfoResult getUserWithUsername(
            1: required string username
        )
    CaasDT.TResult logout(
        1: required string sessionId
    )
    CaasDT.TBoolResult isPermitted(
        1: required string sessionId
        2: required string permission
    )
    CaasDT.TListBoolResult isPermitteds(
        1: required string sessionId
        2: required list<string> permissions
    )
    CaasDT.TBoolResult isPermittedAll(
        1: required string sessionId
        2: required list<string> permissions
    )
    CaasDT.TBoolResult isPermittedUser(
        1: required string username
        2: required string permission
    )
    CaasDT.TListBoolResult isPermittedsUser(
        1: required string username
        2: required list<string> permissions
    )
    CaasDT.TBoolResult isPermittedUserAll(
        1: required string username
        2: required list<string> permissions
    )
    CaasDT.TListStringResult getUserRoles(
        1: required string sessionId
    )
    CaasDT.TBoolResult hasRole(
        1: required string sessionId
        2: required string role
    )
    CaasDT.TListBoolResult hasRoles(
        1: required string sessionId
        2: required list<string> roles
    )
    CaasDT.TBoolResult hasRoleUser(
        1: required string username
        2: required string roleName
    )
    CaasDT.TBoolResult hasAllRoleUser(
        1: required string username
        2: required list<string> roleNames
    )
    CaasDT.TListStringResult getAllUsername()
    CaasDT.TListUserResult getActiveUsername(
        1: required i32 from
        2: required i32 size
    )
    CaasDT.TBoolResult insertUserRoles(
        1: required string username
        2: required set<i32> roleIds
    )
    CaasDT.TBoolResult insertExpirableUserRoles(
        1: required string username
        2: required map<i32, i64> roleIds
    )
    CaasDT.TBoolResult insertUserRole(
        1: required string username
        2: required i32 role
        3: required i64 expireTime
        4: required bool force
    )
    CaasDT.TBoolResult deleteUserRoles(
        1: required string username
        2: required set<i32> roleIds
    )
    CaasDT.TUserInfoPageable  getListUserRole(
        1: optional list<i32> notInRoleIds,
        2: optional list<i32> inRoleIds,
        3: required i32 from,
        4: required i32 size
    )
    CaasDT.TUserInfoPageable  searchListUserRole(
        1: required string usernameSearchKey
        2: optional list<i32> notInRoleIds,
        3: optional list<i32> inRoleIds,
        4: required i32 from,
        5: required i32 size
    )
    CaasDT.TBoolResult resetPasswordUser(
        1: required string username
        2: required string newPassword
    )
    CaasDT.TBoolResult updatePasswordUser(
        1: required string username
        2: required string oldPassword
        3: required string newPassword
    )
    CaasDT.TBoolResult deleteAllExpiredUserRole(
        1: required i32 defaultRoleId,
        2: required i64 maxTime
    )
    CaasDT.TListRoleInfoResult getAllRoleInfo(
        1: required string username
    )
    CaasDT.TListStringResult getAllPermission(
        1: required string username
    )
}
