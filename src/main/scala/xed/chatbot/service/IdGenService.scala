package xed.chatbot.service

import com.twitter.util.Future
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.exception.InternalError

/**
 * @author andy
 * @since 2/11/20
 **/
trait IdGenService {

  def genChallengeId() : Future[Int]

  def genChallengeTemplateId() : Future[String]

  def genMessageId() : Future[Int]

  def genMessageIds(count: Int) : Future[Seq[Int]]
}

case class IdGenServiceImpl(ssdb: SSDB) extends IdGenService {

  override def genChallengeTemplateId(): Future[String] = Future {
    genId("challenge_template").toString
  }


  override def genChallengeId(): Future[Int] = Future {
    1000 + genId("challenge")
  }

  override def genMessageId(): Future[Int] = Future {
    genId("message")
  }

  override def genMessageIds(count: Int): Future[Seq[Int]] = Future {
    for(i <- 0 until count) yield genId("message")
  }

  private def genId(category: String) : Int = {

    val r = ssdb.hincr("xed.id.counter", category,1)
    if(r.ok())
      r.asInt()
    else
      throw InternalError(Some(s"Can't gen id for $category"))
  }
}
