#@namespace scala xed.leaderboard.domain.thrift
struct TLeaderBoard {
    1: required string gameId,
    2: required list<TLeaderBoardItem> topRanking,
    3: optional TLeaderBoardItem myRanking,
    4: required i32 total
}
struct TLeaderBoardItem {
    1: required string username,
    2: required i32 point,
    3: required i32 rank,
    4: optional TShortUserProfile userProfile,
    5: optional string extraData
}
struct TShortUserProfile {
    1:required string username,
    2:optional string firstName,
    3:optional string lastName,
    4:optional string fullName,
    5:optional string avatar
}
struct TPointInfo {
    1: required i32 point,
    2: required i64 startTime,
    3: required i64 duration,
    4: optional string extraData
}
struct TLeaderBoardItemResp {
    1: bool exists,
    2: optional TLeaderBoardItem leaderBoardItem
    3: required i32 total
}
struct TUserPointResp {
    1: bool exists,
    2: string username,
    3: optional TPointInfo point
}
struct TMultiGetUserPointResp {
    1: bool exists,
    2: optional map<string, TPointInfo> userPoints
}
enum TLeaderBoardType {
    Daily,
    Weekly,
    AllTime
}