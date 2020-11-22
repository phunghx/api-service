#@namespace scala xed.leaderboard_mw.service
include "LeaderBoardDT.thrift"
service TLeaderBoardMWService {
    string ping()
LeaderBoardDT.TLeaderBoard getChallengeLeaderBoard(
        1: required string gameId,
        2: required string myUsername,
        3: required i32 from,
        4: required i32 to,
        5: required bool onlyFriendFromSocial,
        6: optional LeaderBoardDT.TLeaderBoardType leaderboardType
    )
    LeaderBoardDT.TLeaderBoardItemResp getChallengeUserRank(
        1: required string gameId,
        2: required string username,
        3: required bool onlyFriendFromSocial,
        4: optional LeaderBoardDT.TLeaderBoardType leaderboardType
    )
    bool setChallengeUserPoint(
        1: required string gameId,
        2: required string username,
        3: required LeaderBoardDT.TPointInfo pointInfo
    )
    bool setChallengeUserPoints(
        1: required string gameId,
        2: required map<string, LeaderBoardDT.TPointInfo> userPoints
    )
    bool setChallengeUserPointIfHighest(
        1: required string gameId,
        2: required string username,
        3: required LeaderBoardDT.TPointInfo pointInfo
    )
    bool setChallengeUserPointsIfHighest(
        1: required string gameId,
        2: required map<string, LeaderBoardDT.TPointInfo> userPoints
    )
    LeaderBoardDT.TUserPointResp getChallengeUserPoint(
        1: required string gameId,
        2: required string username
        3: optional LeaderBoardDT.TLeaderBoardType leaderboardType
    )
    LeaderBoardDT.TMultiGetUserPointResp getChallengeUserPoints(
        1: required string gameId,
        2: required list<string> usernames
        3: optional LeaderBoardDT.TLeaderBoardType leaderboardType
    )
    bool delChallengeUserPoints(
        1: required string gameId,
        2: required list<string> usernames
    )
    bool delChallengeUserPoint(
        1: required string gameId,
        2: required string username
    )
    LeaderBoardDT.TLeaderBoard getSocialLeaderBoard(
        1: required string owner,
        2: required i32 from,
        3: required i32 to
    )
    LeaderBoardDT.TLeaderBoardItemResp getSocialUserRank(
        1: required string owner,
        2: required string username,
    )
    bool setSocialFriendList(
        1: required string owner,
        2: required list<string> friendUserNames
    )
    bool addSocialFriend(
        1: required string owner,
        2: required string username
    )
    bool removeSocialFriend(
        1: required string owner,
        2: required string username
    )
    bool setSocialUserPoint(
        2: required string username,
        3: required LeaderBoardDT.TPointInfo pointInfo
    )
    bool setSocialUserPoints(
        1: required map<string, LeaderBoardDT.TPointInfo> userPoints
    )
    LeaderBoardDT.TUserPointResp getSocialUserPoint(
        1: required string username
    )
    LeaderBoardDT.TMultiGetUserPointResp getSocialUserPoints(
        1: required list<string> usernames
    )
    bool delSocialUserPoint(
        1: required string username
    )
    bool delSocialUserPoints(
        1: required list<string> usernames
    )
    bool setUserProfile(
        1: required LeaderBoardDT.TShortUserProfile userProfile
    )
    bool setUserProfiles(
        1: required list<LeaderBoardDT.TShortUserProfile> userProfiles
    )
}