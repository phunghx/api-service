package xed.api_service.service.statistic

import com.twitter.util.Future
import education.x.commons.SsdbKVS
import javax.inject.Inject
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.SRSSource
import xed.api_service.domain.metric.CountMetric
import xed.api_service.util.Implicits.ScalaFutureLike

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait CountService {
  def updateDueCardCount(username: String, countMap: Map[String,Long]): Future[Boolean]

  def updateFlashCardDueCardCount(username: String, count: Long): Future[Boolean]

  def updateBotDueCardCount(username: String, count: Long): Future[Boolean]

  def getAllSourceDueCardCount(username: String): Future[Map[String,Long]]
}

case class CountServiceImpl@Inject()(ssdb: SSDB) extends CountService {


  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private val flashcardDueKVS = SsdbKVS[String,Long]("xed.count.flashcard.due_cards",ssdb)
  private val botDueKVS = SsdbKVS[String,Long]("xed.count.bot.due_cards",ssdb)

  override def updateBotDueCardCount(username: String, count: Long): Future[Boolean] = {
    botDueKVS.add(username,count).asTwitter
  }

  override def updateFlashCardDueCardCount(username: String, count: Long): Future[Boolean] = {
    flashcardDueKVS.add(username,count).asTwitter
  }

  override def getAllSourceDueCardCount(username: String): Future[Map[String, Long]] = {
    for {
      flashcardCount <- flashcardDueKVS.get(username).map(_.getOrElse(0L)).asTwitter
      botCount <- botDueKVS.get(username).map(_.getOrElse(0L)).asTwitter
    }yield {
      Map(
        SRSSource.FLASHCARD -> flashcardCount,
        SRSSource.BOT -> botCount
      )
    }
  }

  override def updateDueCardCount(username: String, countMap: Map[String, Long]): Future[Boolean] = {
    val flashcardCount = countMap.getOrElse(SRSSource.FLASHCARD,0L)
    val botCount = countMap.getOrElse(SRSSource.BOT,0L)

    for {
      _ <- flashcardDueKVS.add(username, flashcardCount).asTwitter
      _ <- botDueKVS.add(username, botCount).asTwitter
    } yield {
      true
    }

  }
}
