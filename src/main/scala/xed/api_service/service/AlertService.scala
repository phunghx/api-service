package xed.api_service.service

import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.domain.{Alert, AlertTypes, SRSSource}
import xed.api_service.service.statistic.CountService
import xed.userprofile.SignedInUser

trait AlertService extends  Logging {
  lazy val clazz = getClass.getSimpleName

  def getAlerts(user: SignedInUser): Future[Seq[Alert]]

}
/**
 * The very first simple version
 * TODO:
 *
 */
case class SimpleAlertService@Inject()(countService: CountService) extends AlertService {

  override def getAlerts(user: SignedInUser): Future[Seq[Alert]] = {

    countService.getAllSourceDueCardCount(user.username).map(countBySource => {
      Seq(Alert(
        id = 1,
        alertType = AlertTypes.REVIEW,
        recipient = user.username,
        title = s"You have total of ${countBySource.map(_._2).sum} cards to review!",
        description = None,
        fields = Map(
          SRSSource.FLASHCARD -> countBySource.getOrElse(SRSSource.FLASHCARD, 0),
          SRSSource.BOT -> countBySource.getOrElse(SRSSource.BOT, 0)
        )
      ))
    })

  }
}
