package xed.chatbot.service

import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.chatbot.domain.ChatMessage
import xed.chatbot.repository.MessageRepository
import xed.userprofile.SignedInUser


trait MessageService extends  Logging {

  def send(recipient: SignedInUser, messages: Seq[ChatMessage]): Future[Seq[ChatMessage]]

  def multiInsertMessages(recipient: SignedInUser, messages: Seq[ChatMessage]): Future[Boolean]

  def clearMessages(user: SignedInUser): Future[Boolean]

  def getMessages(user: SignedInUser, deleteAfterSuccess: Boolean = false): Future[Seq[ChatMessage]]

  def markAsSeen(getUser: SignedInUser, messageIds: Seq[Int]) : Future[Boolean]


}



case class KiKiBotMessageService(repository: MessageRepository,
                                 idGenService: IdGenService) extends MessageService {

  override def send(recipient: SignedInUser, messages: Seq[ChatMessage]): Future[Seq[ChatMessage]] = {
    for {
      ids <- idGenService.genMessageIds(messages.size)
      sendMessages = messages.zipWithIndex.map { case (message, index) =>
        message.copy(id = ids(index))
      }
      addedOk <-repository.multiInsertMessages(recipient.username, sendMessages)
    } yield addedOk match {
      case true => sendMessages
      case _ => Seq.empty[ChatMessage]
    }
  }

  override def multiInsertMessages(recipient: SignedInUser, messages: Seq[ChatMessage]): Future[Boolean] = {
    repository.multiInsertMessages(recipient.username, messages)
  }



  override def getMessages(user: SignedInUser, deleteAfterSuccess: Boolean = false): Future[Seq[ChatMessage]] = {
    repository.getMessages(user.username, deleteAfterSuccess)
  }

  override def markAsSeen(user: SignedInUser, messageIds: Seq[Int]): Future[Boolean] = {
    repository.removeInboxMessages(user.username,messageIds)
  }

  override def clearMessages(user: SignedInUser) = {
    repository.clearInboxMessages(user.username)
  }
}
