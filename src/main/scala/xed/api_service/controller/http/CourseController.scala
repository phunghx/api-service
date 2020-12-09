package xed.api_service.controller.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.domain.Deck
import xed.api_service.domain.course.LearningCourseInfo
import xed.api_service.domain.response.PageResult
import xed.api_service.service.course.CourseService
import xed.profiler.Profiler
import xed.userprofile.{SessionHolder, UserProfileService}
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper

/**
  * @author tvc12 - thienvc
  * @since  02/03/2020
  */
case class CourseController @Inject()(courseService: CourseService,
                                      profileService: UserProfileService,
                                      sessionHolder: SessionHolder) extends Controller {
  private val clazz = getClass.getSimpleName
  private val apiPath = "/course"

  get(s"$apiPath/me") {
    _: Request => Profiler(s"$clazz.getMyCourses"){
        courseService.getMyCourses(sessionHolder.getUser).flatMap(injectOwnerDetails(_))
    }
  }

  private def injectOwnerDetails(pageResult: PageResult[LearningCourseInfo]): Future[PageResult[LearningCourseInfo]] = {

    val userNames = pageResult.records.flatMap(_.creatorId)

    val injectFn = (requests: Seq[LearningCourseInfo], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        request.creator = request.creatorId.flatMap(users.get(_))
      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(pageResult.records, shortProfiles)
    } yield {
      pageResult.copy(
        records = r
      )
    }
  }

}
