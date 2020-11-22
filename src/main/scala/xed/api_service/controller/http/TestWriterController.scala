package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import xed.api_service.service.SRSService
import xed.userprofile.SessionHolder


class TestWriterController@Inject()(srsService: SRSService,
                                    sessionHolder: SessionHolder) extends Controller {

  get("/test/string") {
    req: Request => {
      "1 chuỗi ngắn"
    }
  }

  get("/test/object") {
    req: Request => {
      case class TestObj(msg: String)
      TestObj("the data")
    }
  }
  get("/test/error") {
    req: Request => {
      throw new Exception("error message")
    }
  }


}
