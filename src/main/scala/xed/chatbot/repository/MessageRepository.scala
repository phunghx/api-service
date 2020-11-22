package xed.chatbot.repository

import com.twitter.inject.Logging
import com.twitter.util.Future
import education.x.commons.{SsdbKVS, SsdbSortedSet}
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.exception.InternalError
import xed.api_service.domain.response.PageResult
import xed.api_service.util.JsonUtils
import xed.chatbot.domain.ChatMessage
import xed.api_service.util.Implicits._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait MessageRepository extends Logging {

  def multiInsertMessages(username: String, messages: Seq[ChatMessage]): Future[Boolean]

  def getMessages(username: String, deleteAfterSuccess: Boolean = false): Future[Seq[ChatMessage]]

  def clearInboxMessages(username: String): Future[Boolean]

  def removeInboxMessages(username: String, messageIds: Seq[Int]): Future[Boolean]

}

case class SSDBMessageRepository(ssdb: SSDB, inboxKey: String) extends  MessageRepository {


  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  override def multiInsertMessages(username: String,
                                   messages: Seq[ChatMessage]): Future[Boolean] = {

    val idZSet = SsdbSortedSet(s"$inboxKey.ids.$username", ssdb)
    val messageKVS = SsdbKVS[String, ChatMessage](s"$inboxKey.messages.$username", ssdb)

    val entries = messages.map(msg => (msg.id.toString, msg.id.toLong)).toArray
    for {
      _ <- idZSet.madd(entries).asTwitter.map({
        case true => true
        case _ => throw InternalError(Some("Can't send insert all messages to repo" + JsonUtils.toJson(messages, false)))
      })
      isOK <- messageKVS.multiAdd(messages.map(e => (e.id.toString, e)).toArray).asTwitter
    } yield isOK
  }

  override def clearInboxMessages(username: String) = {
    val idZSet = SsdbSortedSet(s"$inboxKey.ids.$username", ssdb)
    val messageKVS = SsdbKVS[String, ChatMessage](s"$inboxKey.messages.$username", ssdb)

    for {
      _ <- idZSet.clear().asTwitter
      isOK <- messageKVS.clear().asTwitter
    } yield {
      isOK
    }
  }


  override def getMessages(username: String, deleteAfterSuccess: Boolean = false): Future[Seq[ChatMessage]] = {
    for {
      messageResult <- getInboxMessages(username)
      _ <- if (deleteAfterSuccess) {
        removeInboxMessages(username, messageResult.records.map(_.id))
      } else Future.value(0)
    } yield {
      messageResult.records
    }

  }

  private def getInboxMessages(username: String) = {
    val idZSet = SsdbSortedSet(s"$inboxKey.ids.$username", ssdb)
    val messageKVS = SsdbKVS[String, ChatMessage](s"$inboxKey.messages.$username", ssdb)

    for {
      count <- idZSet.size().map(_.getOrElse(0)).asTwitter
      ids <- idZSet.range(0, count, reverseOrder = true)
        .map(_.getOrElse(Array.empty))
        .map(_.map(_._1))
        .asTwitter
      messageMap <- messageKVS.multiGet(ids)
        .map(_.getOrElse(Map.empty))
        .asTwitter

    } yield {
      val messages = ids.map(messageMap.get(_))
        .filter(_.isDefined)
        .map(_.get)

      PageResult(count, messages)
    }
  }

  override def removeInboxMessages(username: String, messageIds: Seq[Int]): Future[Boolean] = {
    val idZSet = SsdbSortedSet(s"$inboxKey.ids.$username", ssdb)
    val messageKVS = SsdbKVS[String, ChatMessage](s"$inboxKey.messages.$username", ssdb)

    for {
      ids <- Future.value(messageIds.map(_.toString).toArray)
      _ <- idZSet.mremove(ids).asTwitter
      r <- messageKVS.multiRemove(ids).asTwitter
    } yield {
      r
    }

  }

}




