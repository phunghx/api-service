package xed.api_service.service

import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw}
import javax.inject.Inject
import xed.api_service.domain.ReviewInfo
import xed.api_service.domain.request.ReviewRequest
import xed.api_service.repository.ReviewHistoryRepository
import xed.userprofile.SignedInUser

trait HistoryService extends Logging {

  def recordError(user: SignedInUser, request: ReviewRequest, model: Option[ReviewInfo]) : Future[Boolean]

  def recordSuccess(user: SignedInUser, request: ReviewRequest, model: Option[ReviewInfo]) : Future[Boolean]
}

case class HistoryServiceImpl@Inject()(repository: ReviewHistoryRepository) extends HistoryService {

  override def recordSuccess(user: SignedInUser, request: ReviewRequest, model: Option[ReviewInfo]): Future[Boolean] = {
    val history = request.build(true,user, model)
    val fn = for {
      r <- repository.insert(history,false).map(_.isDefined)
    } yield r

    fn.transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        error("recordSuccess", e)
        Future.False
    })
  }

  override def recordError(user: SignedInUser, request: ReviewRequest, model: Option[ReviewInfo]): Future[Boolean] = {
    val history = request.build(false,user,model)
    repository.insert(history,false).map(_.isDefined).transform({
      case Return(r) => Future.value(r)
      case Throw(e) =>
        error("recordError", e)
        Future.False
    })
  }
}
