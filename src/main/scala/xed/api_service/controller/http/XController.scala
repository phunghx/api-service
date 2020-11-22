package xed.api_service.controller.http

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag

abstract class XController extends Controller {
  protected lazy val clazz = getClass.getSimpleName

  @Inject(optional = true)
  @Flag("profiler_sk")
  var secretKey: String = "xed@masteri"
}
