package xed.chatbot.controllers.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import xed.api_service.controller.http.XController
import xed.chatbot.service.LearnService
import xed.profiler.Profiler
import xed.userprofile.SessionHolder

case class BotAdminController @Inject()(learnService: LearnService,
                                        sessionHolder: SessionHolder) extends XController {


  get("/bot/admin/learn/stats") {
      request: Request =>
        Profiler(s"$clazz.stats") {
          learnService
            .getStatistics(request.getParam("email"))
        }
    }

  post("/bot/admin/learn/reset") {
    request: Request => Profiler(s"$clazz.reset") {
        learnService.resetLearningCourse(
          request.getParam("email"),
          request.getBooleanParam("is_full_reset", false)
        )
      }
  }
}
