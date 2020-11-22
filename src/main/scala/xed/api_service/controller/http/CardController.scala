package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import xed.api_service.domain.SRSSource
import xed.api_service.domain.request._
import xed.api_service.service.{CardService, SRSService}
import xed.profiler.Profiler
import xed.userprofile.SessionHolder

case class CardController @Inject()(cardService: CardService,
                                    srsService: SRSService,
                                    sessionHolder: SessionHolder)  extends  Controller {
  private val clazz = getClass.getSimpleName
  private val apiPath = "/card"

  put(s"$apiPath/:card_id") {
    request: EditCardRequest => Profiler(s"$clazz.editCard"){
      cardService.editCard(sessionHolder.getUser, request)
    }
  }

  get(s"$apiPath/:card_id") {
    request: Request => Profiler(s"$clazz.getCard") {
      val cardId = request.getParam("card_id")
      cardService.getCard(cardId)
    }
  }

  post(s"$apiPath/list") {
    request: GetCardRequest => Profiler(s"$clazz.getCards") {
      cardService.getCardAsMap(request.cardIds)
    }
  }

  post(s"$apiPath/detail/list") {
    request: GetCardRequest => Profiler(s"$clazz.getReviewCardAsList") {
      cardService.getReviewCardAsList(
        sessionHolder.getUser,
        SRSSource.FLASHCARD,
        request.cardIds
      )
    }
  }

  post(s"$apiPath/detail/tab") {
    request: GetCardRequest => Profiler(s"$clazz.getReviewCardAsTab") {
      cardService.getReviewCardAsMap(
        sessionHolder.getUser,
        SRSSource.FLASHCARD,
        request.cardIds
      )
    }
  }

  delete(s"$apiPath/:card_id") {
    request: Request =>  Profiler(s"$clazz.deleteCard") {
      val cardId = request.getParam("card_id")
      cardService.deleteCard(sessionHolder.getUser,cardId)
    }
  }

}
