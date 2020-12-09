package xed.chatbot.repository

import com.twitter.util.Future
import org.nutz.ssdb4j.spi.SSDB

import scala.collection.JavaConversions._
case class ChallengeHistoryRepository(ssdb: SSDB) {


  val joinedChallengeKey = "xed.challenge.joined.%s"


  def joinChallenge(username: String, challengeId: Int) : Future[Boolean] = Future {
    val r = ssdb.zset(
      joinedChallengeKey.format(username),
      challengeId,
      System.currentTimeMillis()
    )

    r.ok()
  }


  def getJoinChallengeIds(username: String) = Future {
    val key = joinedChallengeKey.format(username)
    val total = ssdb.zsize(key).asInt()
    val r = ssdb.zrrange(key, 0, total)

    val challengeIds = if (!r.ok())
      Seq.empty
    else {
      r.listString()
        .grouped(2)
        .map(arr =>(arr(0).toInt, arr(1).toLong))
        .toSeq
    }

    (challengeIds, total)
  }

  def getJoinChallengeIds(username: String, from: Int, size: Int) : Future[(Seq[Int], Int)] = Future {
    val key = joinedChallengeKey.format(username)
    val total = ssdb.zsize(key).asInt()
    val r = ssdb.zrange(key, from, size)

    val challengeIds = if (!r.ok())
      Seq.empty
    else {
      r.listString()
        .grouped(2)
        .map(_.get(0))
        .map(_.toInt)
        .toSeq
    }

    (challengeIds, total)

  }

}




