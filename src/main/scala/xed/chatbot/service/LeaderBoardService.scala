package xed.chatbot.service

import com.twitter.util.Future
import xed.api_service.domain.exception.InternalError
import xed.api_service.util.JsonUtils
import xed.chatbot.domain.leaderboard.LeaderBoardItem
import xed.leaderboard.domain.thrift.{TLeaderBoardType, TPointInfo, TShortUserProfile}
import xed.leaderboard_mw.service.TLeaderBoardMWService
import xed.userprofile.UserProfileService

/**
 * @author andy
 * @since 3/16/20
 **/
trait LeaderBoardService {

  def initialPointIfNotFound(username: String, challengeId: String): Future[Boolean]

  def setUserShortProfile(username: String): Future[Boolean]

  def setPoint(gameId: String,
               username: String,
               startTime: Long,
               duration: Long,
               totalQuestions: Int,
               totalAnsweredQuestions: Int,
               points: Int): Future[Boolean]

  def setPointIfHighest(gameId: String,
                        username: String,
                        startTime: Long,
                        duration: Long,
                        totalQuestions: Int,
                        totalAnsweredQuestions: Int,
                        points: Int): Future[Boolean]

  def getRank(gameId: String,
              username: String,
              leaderboardType: Option[TLeaderBoardType]): Future[LeaderBoardItem]

  def hasChallengePoint(gameId: String, username: String): Future[Boolean]
}


case class LeaderBoardServiceImpl(client: TLeaderBoardMWService.MethodPerEndpoint,
                                  userProfileService: UserProfileService) extends  LeaderBoardService {

  override def hasChallengePoint(gameId: String, username: String): Future[Boolean] = {
    client.getChallengeUserRank(
      gameId,
      username,
      false).map(r => r.leaderBoardItem match {
      case Some(leaderBoardItem) if leaderBoardItem.point < 0 => false
      case Some(leaderBoardItem) => true
      case _ => false
    })
  }


  override def initialPointIfNotFound(username: String, challengeId: String): Future[Boolean] = {
    for {
      alreadyInitScore <- hasChallengePoint(challengeId, username)
      initSuccess <- if(!alreadyInitScore) {
        setPointIfHighest(
          challengeId,
          username,
          System.currentTimeMillis(),
          0,
          0,
          0,
          -1)
      } else {
        Future.True
      }
    } yield {
      initSuccess
    }
  }

  override def setPoint(gameId: String,
                        username: String,
                        startTime: Long,
                        duration: Long,
                        totalQuestions: Int,
                        totalAnsweredQuestions: Int,
                        points: Int): Future[Boolean] = {
    val point = TPointInfo(points,
      startTime,
      duration,
      extraData = Some(JsonUtils.toJson(Map(
        "total_questions" -> totalQuestions,
        "total_answered_questions" -> totalAnsweredQuestions
      )))
    )
    client.setChallengeUserPoint(
      gameId,
      username,
      point
    )
  }

  override def setPointIfHighest(gameId: String,
                                 username: String,
                                 startTime: Long,
                                 duration: Long,
                                 totalQuestions: Int,
                                 totalAnsweredQuestions: Int,
                                 points: Int): Future[Boolean] = {
    val point = TPointInfo(points,
      startTime,
      duration,
      extraData = Some(JsonUtils.toJson(Map(
        "total_questions" -> totalQuestions,
        "total_answered_questions" -> totalAnsweredQuestions
      )))
    )
    client.setChallengeUserPointIfHighest(
      gameId,
      username,
      point
    )
  }

  override def setUserShortProfile(username: String): Future[Boolean] = {
    userProfileService.getProfile(username).flatMap({
      case Some(profile) =>
        client.setUserProfile(TShortUserProfile(
          username,
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          avatar = profile.avatar
        ))
      case _ => Future.False
    })
  }



  override def getRank(gameId: String,
                       username: String,
                       leaderboardType: Option[TLeaderBoardType]): Future[LeaderBoardItem] = {
    import LeaderBoardItem._
    client.getChallengeUserRank(
      gameId,
      username,
      false,
      leaderboardType).map(r => {
      r.leaderBoardItem match {
        case Some(leaderBoardItem) => leaderBoardItem.toLeaderBoardItem
        case _ => throw InternalError(Some("No Rank was found."))
      }
    })
  }
}

