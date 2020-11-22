package xed.profiler.controller.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag
import xed.profiler.Profiler

/**
  * @author anhlt
  */
class ProfilerController extends Controller {
  @Inject(optional = true)
  @Flag("profiler_sk")
  var secretKey: String = "xed@masteri"

  get("/_profiler") {
    request: Request => {
      if (request.getParam("sk", "").equals(secretKey))
        response.ok.view("profiler.mustache", Profiler.getProfilerViewData)
      else
        response.ok.html("")
    }
  }

  post("/_profiler/clear_all") { request: Request =>
  {
    if (request.getParam("sk", "").equals(secretKey))
      response.ok.html(Profiler.clearAll())
    else
      response.ok.html("")
  }
  }
}
