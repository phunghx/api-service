package xed.api_service.controller.http.filter.parser

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.controller.http.filter.DataRequestContext
import xed.api_service.domain.request.SearchRequest
import xed.api_service.util.JsonUtils

case class GlobalDeckSearchRequest(query: Option[String], searchRequest: SearchRequest)  {

}

class SearchDeckRequestParser @Inject()() extends SimpleFilter[Request, Response] with Logging {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val query = Option(request.getParam("query", null))
    val searchRequest = JsonUtils.fromJson[SearchRequest](request.contentString)

    DataRequestContext.setDataRequest(request, GlobalDeckSearchRequest(query,searchRequest))
    service(request)
  }
}