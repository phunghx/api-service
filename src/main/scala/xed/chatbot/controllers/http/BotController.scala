package xed.chatbot.controllers.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.util.Future
import xed.api_service.controller.http.XController
import xed.api_service.controller.http.filter.DataRequestContext.MainRequestContextSyntax
import xed.chatbot.controllers.http.parsers.ChatRequestParser
import xed.chatbot.domain.{ChatMessage, ChatRequest}
import xed.chatbot.service.GatewayService
import xed.profiler.Profiler
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class BotController @Inject()(gatewayService: GatewayService,
                                   profileService: UserProfileService,
                                   sessionHolder: SessionHolder) extends XController {

  post("/bot/start") {
    request: Request =>
      Profiler(s"$clazz.start") {
        gatewayService.conversationStart(sessionHolder.getUser)
      }
  }

  post("/bot/end") {
    request: Request =>
      Profiler(s"$clazz.end") {
        gatewayService.conversationEnd(sessionHolder.getUser)
      }
  }

  filter[ChatRequestParser]
    .post("/bot/send") {
      request: Request =>
        Profiler(s"$clazz.send") {
          gatewayService
            .chat(sessionHolder.getUser, request.requestData[ChatRequest])
            .flatMap(enhanceOwnerDetail(_))
        }
    }

  private def enhanceOwnerDetail(chatMessage: ChatMessage): Future[ChatMessage] = {
    val userNames = Seq(chatMessage.sender, chatMessage.recipient)
      .filter(_.isDefined)
      .map(_.get)

    for {
      userProfiles <- profileService.getProfiles(userNames)
      users = userProfiles.map(e => e._1 -> e._2.toShortProfile)

    } yield {
      if (chatMessage.sender.isDefined) {
        chatMessage.senderDetail = users.get(chatMessage.sender.get)
      }

      if (chatMessage.recipient.isDefined) {
        chatMessage.recipientDetail = users.get(chatMessage.recipient.get)
      }
      chatMessage
    }
  }

}
