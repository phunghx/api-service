package xed.chatbot.domain.leaderboard

import xed.leaderboard.domain.thrift.TLeaderBoardItem
import xed.userprofile.domain.ShortUserProfile

/**
 * @author andy
 * @since 3/17/20
 **/
case class LeaderBoardItem(username: String,
                           point: Int,
                           rank:Int,
                           userProfile: Option[ShortUserProfile])

object LeaderBoardItem {

  implicit class LeaderBoardItemLike(item: TLeaderBoardItem) {
    def toLeaderBoardItem() = {
      LeaderBoardItem(
        item.username,
        item.point,
        item.rank,
        item.userProfile.map(tProfile => ShortUserProfile(
          tProfile.username,
          tProfile.firstName,
          tProfile.lastName,
          tProfile.fullName,
          tProfile.avatar
        ))
      )
    }
  }
}
