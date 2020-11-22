package xed.api_service.controller.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.Inject
import xed.api_service.controller.http.filter.LoggedInUserFilter
import xed.chatbot.domain.challenge.{CreateChallengeFromCourseRequest, CreateChallengeFromDeckRequest, UpdateChallengeFromDeckRequest}
import xed.chatbot.service.ChallengeService
import xed.profiler.Profiler
import xed.userprofile.SessionHolder

/**
  * @author andy
  * @since  02/03/2020
  */
case class ChallengeController @Inject()(challengeService: ChallengeService,
                                         sessionHolder: SessionHolder) extends Controller {
  private val clazz = getClass.getSimpleName
  private val apiPath = "/challenge"

  filter[LoggedInUserFilter]
    .post(s"$apiPath/create_from_course") {
      request: CreateChallengeFromCourseRequest =>
        Profiler(s"$clazz.createFromCourse") {
          challengeService.createChallengeFromCourse(
            sessionHolder.getUser.username,
            request
          )
        }
    }

  filter[LoggedInUserFilter]
    .post(s"$apiPath/create_from_deck") {
      request: CreateChallengeFromDeckRequest =>
        Profiler(s"$clazz.createFromDeck") {
          challengeService.createChallengeFromDeck(
            sessionHolder.getUser.username,
            request
          )
        }
    }


  filter[LoggedInUserFilter]
  .put(s"$apiPath/:challenge_id/update_from_deck") {
    request: UpdateChallengeFromDeckRequest =>  Profiler(s"$clazz.updateFromDeck") {
      challengeService.updateChallengeFromDeck(
        request.request.getIntParam("challenge_id"),
        request
      )
    }
  }

  get(s"$apiPath/:challenge_id") {
    request: Request => {
      challengeService.getChallengeInfo(request.getIntParam("challenge_id"))
    }
  }

  filter[LoggedInUserFilter]
    .get(s"$apiPath/me") {
      _: Request => Profiler(s"$clazz.getMyChallenges") {
          challengeService.getMyChallenges(sessionHolder.getUser.username)
        }
    }
}
