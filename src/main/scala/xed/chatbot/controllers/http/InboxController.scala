package xed.chatbot.controllers.http

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.util.Future
import xed.api_service.controller.http.XController
import xed.chatbot.domain.{ChatMessage, MarkMessageReadRequest}
import xed.chatbot.service.MessageService
import xed.profiler.Profiler
import xed.userprofile.domain.ShortUserProfile
import xed.userprofile.domain.UserProfileImplicits.UserProfileWrapper
import xed.userprofile.{SessionHolder, UserProfileService}

case class InboxController @Inject()(messageService: MessageService,
                                     profileService: UserProfileService,
                                     sessionHolder: SessionHolder) extends XController {

  post("/bot/inbox/new") {
    _: Request =>
      Profiler(s"$clazz.getNewMessageThenDelete") {
        messageService.getMessages(
          sessionHolder.getUser,
          deleteAfterSuccess = true).flatMap(enhanceOwnerDetails)
      }
  }

  post("/bot/inbox/unread") {
    _: Request =>
      Profiler(s"$clazz.getUnread") {
        messageService.getMessages(
          sessionHolder.getUser,
          deleteAfterSuccess = false).flatMap(enhanceOwnerDetails)
      }
  }

  post("/bot/inbox/seen") {
    request: MarkMessageReadRequest =>
      Profiler(s"$clazz.markAsSeen") {
        messageService.markAsSeen(
          sessionHolder.getUser,
          request.messageIds)
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


  private def enhanceOwnerDetails(chatMessages: Seq[ChatMessage]): Future[Seq[ChatMessage]] = {
    val userNames = chatMessages
      .flatMap(x => Seq(x.sender, x.recipient))
      .filter(_.isDefined)
      .map(_.get)

    val injectFn = (requests: Seq[ChatMessage], users: Map[String, ShortUserProfile]) => {
      requests.foreach(request => {
        if (request.sender.isDefined) {
          request.senderDetail = users.get(request.sender.get)
        }

        if (request.recipient.isDefined) {
          request.recipientDetail = users.get(request.recipient.get)
        }

      })
      requests
    }

    for {
      userProfiles <- profileService.getProfiles(userNames)
      shortProfiles = userProfiles.map(e => e._1 -> e._2.toShortProfile)
      r = injectFn(chatMessages, shortProfiles)
    } yield r
  }
}
