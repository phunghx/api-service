package xed.api_service.controller.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.Inject
import xed.api_service.controller.http.filter.LoggedInUserFilter
import xed.chatbot.domain.challenge.CreateChallengeTemplateFromCourseRequest
import xed.chatbot.service.ChallengeTemplateService
import xed.profiler.Profiler
import xed.userprofile.SessionHolder

/**
  * @author andy
  * @since  02/03/2020
  */
case class ChallengeTemplateController @Inject()(challengeTemplateService: ChallengeTemplateService,
                                                 sessionHolder: SessionHolder) extends Controller {
  private val clazz = getClass.getSimpleName
  private val apiPath = "/challenge/template"


  filter[LoggedInUserFilter]
    .post(s"$apiPath/create_from_course") {
      request: CreateChallengeTemplateFromCourseRequest =>
        Profiler(s"$clazz.createChallengeTemplateFromCourse") {
          challengeTemplateService.createChallengeTemplateFromCourse(
            sessionHolder.getUser.username,
            request)
        }
    }

  get(s"$apiPath/:template_id") {
    request: Request => Profiler(s"$clazz.getChallengeTemplate") {
      challengeTemplateService.getChallengeTemplate(request.getParam("template_id"))
    }
  }

  delete(s"$apiPath/:template_id") {
    request: Request => Profiler(s"$clazz.deleteChallengeTemplate") {
      challengeTemplateService.deleteChallengeTemplate(request.getParam("template_id"))
    }
  }

}
