package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future
import xed.api_service.domain.course.JourneyInfo
import xed.api_service.domain.request._
import xed.api_service.domain.response.PageResult
import xed.api_service.module.BaseResponse
import xed.api_service.service.course.{CategoryService, JourneyService}
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class XJourneyController @Inject()(journeyService: JourneyService,
                                        categoryService: CategoryService,
                                        profileService: UserProfileService,
                                        sessionHolder: SessionHolder) extends Controller {

  post("/journey/category") {
    request: CreateXCategoryRequest => {
      categoryService.create(request)
    }
  }

  put("/journey/category/:id") {
    request: UpdateXCategoryRequest => {
      categoryService.update(sessionHolder.getUser,request).map(r => BaseResponse(r,Some(r),None))
    }
  }

  get("/journey/category/all") {
    request: ListAllCategoryRequest => {
      categoryService.listAll(request.status)
    }
  }

  get("/journey/category/:id") {
    request: Request => {
      val id = request.getParam("id")
      categoryService.get(id).map(x => BaseResponse(x.isDefined,x,None))
    }
  }

  delete("/journey/category/:id") {
    request: Request => {
      val id = request.getParam("id")
      categoryService.delete(sessionHolder.getUser, id).map(r => BaseResponse(r,Some(r),None))
    }
  }



  post("/journey") {
    request: CreateJourneyRequest => {
      journeyService.create(sessionHolder.getUser,request).flatMap(enhanceOwnerDetail(_))
    }
  }


  put("/journey/:journey_id") {
    request: UpdateJourneyRequest => {
      journeyService.update(sessionHolder.getUser, request)
    }
  }

  get("/journey/:journey_id") {
    request: Request => {
      val id = request.getParam("journey_id")
      journeyService.get(id)
        .flatMap(x => if(x.isDefined) enhanceOwnerDetail(x.get).map(Some(_)) else Future.None)
        .map(r => BaseResponse(r.nonEmpty,r,None))
    }
  }

  post("/journey/:journey_id/publish") {
    request: PublishJourneyRequest => {
      journeyService.publish(sessionHolder.getUser,request)
    }
  }


  delete("/journey/:id") {
    request: Request => {
      val id = request.getParam("id")
      journeyService.delete(sessionHolder.getUser, id).map(r => BaseResponse(r,Some(r),None))
    }
  }


  post("/journey/me/search") {
    request: SearchRequest => {
      val user = sessionHolder.getUser
      journeyService.searchMyJourneys(user,request)
        .flatMap(r => enhanceOwnerDetails(r.records).map(PageResult(r.total,_)))
    }
  }

  post("/journey/search") {
    request: SearchRequest => {
      journeyService.search(request)
        .flatMap(r => enhanceOwnerDetails(r.records).map(PageResult(r.total,_)))
    }
  }


  private def enhanceOwnerDetail(journey:JourneyInfo): Future[JourneyInfo] = {
    for {
      userProfile <- if(journey.creator.isDefined) profileService.getProfile(journey.creator.get)
      else Future.value(None)
      _ =  {
        journey.creatorDetail = userProfile.map(_.toShortProfile)
      }

    } yield journey
  }


  private def enhanceOwnerDetails(journeys: Seq[JourneyInfo]): Future[Seq[JourneyInfo]] = {
    val userNames = journeys.flatMap(_.creator)

    val injectFn = (requests: Seq[JourneyInfo], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        if (request.creator.isDefined) {
          request.creatorDetail = users.get(request.creator.get)
        }
      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(journeys, shortProfiles)
    } yield r
  }

}
