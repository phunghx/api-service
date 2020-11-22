package xed.chatbot.controllers.http.parsers

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import javax.inject.Inject
import xed.api_service.controller.http.filter.DataRequestContext
import xed.api_service.util.JsonUtils
import xed.chatbot.domain._
import xed.userprofile.SessionHolder

/**
  * @author anhlt
  */


class ChatRequestParser @Inject()(sessionHolder: SessionHolder) extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {

    val requestBody = JsonUtils.fromJson[ChatBodyRequest](request.contentString)

    val data = ChatRequest(
      sender = sessionHolder.getUser,
      messageType= if(requestBody.messageType.isDefined) requestBody.messageType
      else Some(MessageType.TEXT),
      message = requestBody.message,
      currentAction = requestBody.currentAction,
      languageCode = None,
      createdTime = Some(System.currentTimeMillis())
    )
    DataRequestContext.setDataRequest(request, data)
    service(request)
  }
}

class LearnCardRequestParser @Inject()(sessionHolder: SessionHolder) extends SimpleFilter[Request, Response] {
  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {

    val requestBody = JsonUtils.fromJson[LearnCardBodyRequest](request.contentString)

    val data = LearnCardRequest(
      sender = sessionHolder.getUser,
      cardId = requestBody.cardId,
      currentAction = requestBody.currentAction,
      createdTime = Some(System.currentTimeMillis())
    )
    DataRequestContext.setDataRequest(request, data)
    service(request)
  }
}
