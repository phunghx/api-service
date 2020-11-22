package xed.api_service.controller.http

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class Resources extends Controller {
  get("/public/:*") {
    request: Request => response.ok.file(s"/public/${request.params("*")}")
  }
}