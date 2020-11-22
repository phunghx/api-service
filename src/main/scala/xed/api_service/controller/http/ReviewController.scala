package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import xed.api_service.domain.SRSSource
import xed.api_service.domain.request._
import xed.api_service.service.SRSService
import xed.profiler.Profiler
import xed.userprofile.SessionHolder

case class ReviewController @Inject()(srsService: SRSService,
                                      sessionHolder: SessionHolder)  extends  Controller {
  private val clazz = getClass.getSimpleName

  private val apiPath = "/srs"
  private val flashcardSource = SRSSource.FLASHCARD

  get(s"$apiPath/card/due") {
    request: Request => {
      srsService.getTotalDueCardBySource(sessionHolder.getUser)
    }
  }

  post(s"$apiPath/card/:card_id") {
    request: Request =>
      Profiler(s"$clazz.addCard") {
        val cardId = request.getParam("card_id")
        srsService.add(sessionHolder.getUser, flashcardSource, cardId)
      }
  }

  post(s"$apiPath/card") {
    request: AddSRSRequest =>
      Profiler(s"$clazz.addCards") {
        srsService.multiAdd(sessionHolder.getUser, flashcardSource, request.cardIds)
      }
  }

  post(s"$apiPath/card/:card_id/review") {
    request: ReviewRequest =>
      Profiler(s"$clazz.submitReview") {
        srsService.review(sessionHolder.getUser, flashcardSource, request)
      }
  }

  post(s"$apiPath/card/:card_id/learn_again") {
    request: Request =>
      Profiler(s"$clazz.learnAgain") {
        val cardId = request.getParam("card_id")
        srsService.learnAgain(sessionHolder.getUser, flashcardSource, cardId)
      }
  }

  post(s"$apiPath/card/:card_id/move_done") {
    request: Request =>
      Profiler(s"$clazz.moveToDone") {
        val cardId = request.getParam("card_id")
        srsService.makeDone(sessionHolder.getUser, flashcardSource, cardId)
      }
  }

  post(s"$apiPath/card/:card_id/ignore") {
    request: Request =>
      Profiler(s"$clazz.ignore") {
        val cardId = request.getParam("card_id")
        srsService.ignore(sessionHolder.getUser, flashcardSource, cardId)
      }
  }

  delete(s"$apiPath/card/:card_id") {
    request: Request =>
      Profiler(s"$clazz.deleteCard") {
        val cardId = request.getParam("card_id")
        srsService.delete(sessionHolder.getUser, flashcardSource, cardId)
      }
  }

  post(s"$apiPath/card/list/delete") {
    request: RemoveFromSRSRequest =>
      Profiler(s"$clazz.deleteReviewInfos") {
        srsService.multiDelete(sessionHolder.getUser.username, flashcardSource, request.cardIds)
      }
  }


  post(s"$apiPath/list") {
    request: GetCardRequest =>
      Profiler(s"$clazz.getReviewInfos") {
        srsService.getReviewInfo(sessionHolder.getUser, flashcardSource, request.cardIds)
      }
  }


  post(s"$apiPath/card/list") {
    request: GetCardRequest =>
      Profiler(s"$clazz.getReviewCards") {
        srsService.getReviewCards(sessionHolder.getUser, flashcardSource, request.cardIds)
      }
  }


  post(s"$apiPath/search/deck/due") {
    request: SearchRequest =>
      Profiler(s"$clazz.searchDueDeck") {
        srsService.getDueDecks(sessionHolder.getUser, flashcardSource, request)
      }
  }

  post(s"$apiPath/search/deck/learning") {
    request: SearchRequest =>
      Profiler(s"$clazz.searchLearningDeck") {
        srsService.getLearningDecks(sessionHolder.getUser, flashcardSource, request)
      }
  }


  post(s"$apiPath/search/deck/done") {
    request: SearchRequest =>
      Profiler(s"$clazz.searchDoneDeck") {
        srsService.getDoneDecks(sessionHolder.getUser, flashcardSource, request)
      }
  }


  post(s"$apiPath/card/search/due") {
    request: SearchRequest => {
      srsService.searchDueCards(sessionHolder.getUser, flashcardSource, request)
        .map(r => r.copy(records = r.records.map(_.model).filter(_.isDefined).map(_.get)))
    }
  }

  post(s"$apiPath/card/search/learning") {
    request: SearchRequest => {
      srsService.searchLearningCards(sessionHolder.getUser, flashcardSource, request)
        .map(r => r.copy(records = r.records.map(_.model).filter(_.isDefined).map(_.get)))
    }
  }

  post(s"$apiPath/card/search/done") {
    request: SearchRequest => {
      srsService.searchDoneCards(sessionHolder.getUser, flashcardSource, request)
        .map(r => r.copy(records = r.records.map(_.model).filter(_.isDefined).map(_.get)))
    }
  }

  post(s"$apiPath/card/search") {
    request: SearchRequest => {
      srsService.search(request, flashcardSource)
        .map(r => r.copy(records = r.records.map(_.model).filter(_.isDefined).map(_.get)))
    }
  }
}
