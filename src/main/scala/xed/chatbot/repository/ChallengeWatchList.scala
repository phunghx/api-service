package xed.chatbot.repository

import org.nutz.ssdb4j.spi.SSDB

import scala.collection.JavaConversions._

trait ChallengeWatchList {

  def add(challengeId: Int, dueTime: Long) : Boolean

  def peek(): Option[(Int,Long)]

  def remove(challengeId: Int) : Boolean

}

case class ChallengeWatchListImpl(ssdb: SSDB) extends ChallengeWatchList {

  val challengeWatchlistKey = "xed.challenge.watch_list"

  override def add(challengeId: Int, dueTime: Long): Boolean = {
    val r = ssdb.zset(
      challengeWatchlistKey,
      challengeId,
      dueTime
    )
    r.ok()
  }

  override def peek(): Option[(Int, Long)] = {
    val r = ssdb.zrange(challengeWatchlistKey, 0, 1)
    if (!r.ok())
      None
    else {
      r.listString()
        .grouped(2)
        .map(e => e(0).toInt -> e(1).toLong)
        .toSeq
        .headOption
    }
  }

  override def remove(challengeId: Int): Boolean = {
    val r = ssdb.zdel(challengeWatchlistKey, challengeId)
    r.ok()
  }
}




