package xed.chatbot.service

import com.twitter.inject.Logging
import com.twitter.util.{Future, FuturePool}
import xed.api_service.util.Implicits.FutureEnhance
import xed.chatbot.domain.challenge.Challenge
import xed.chatbot.repository.{ChallengeRepository, ChallengeWatchList}
import xed.profiler.Profiler

/**
 * @author andy
 * @since 2/28/20
 **/
trait ChallengeWatcher extends  Logging {
  lazy val clazz = getClass.getSimpleName

  def start() : Future[Unit]

  def watch(challenge: Challenge): Future[Boolean]

}

case class ExpireChallengeWatcher(watchList: ChallengeWatchList,
                                  repository: ChallengeRepository) extends ChallengeWatcher {

  implicit val futurePool = FuturePool.unboundedPool
  private val sync = new Object()
  private var isWatching = false



  override def watch(challenge: Challenge): Future[Boolean] = Future {

    challenge.canExpired match {
      case true =>
        val ok = watchList.add(
          challenge.challengeId,
          challenge.dueTime.getOrElse(0L)
        )

        if (ok) {
          notifyChanged(challenge)
        }

        ok
      case _ => true
    }

  }

  override def start(): Future[Unit] = futurePool {
    sync.synchronized{
      if(!isWatching) {
        isWatching = true
        do{
          tryCloseExpiredChallenges()
        }while(true)
      }
    }
  }

  private def tryCloseExpiredChallenges(): Unit = Profiler(s"$clazz.tryCloseExpiredChallenges") {
     sync.synchronized{
       try {

         val now = System.currentTimeMillis()
         watchList.peek() match {
           case Some((challengeId, dueTime)) if dueTime <= now =>
             //Close this challenge and then remove it from watch_list

             val closeOK = closeChallenge(challengeId)
             removeFromGlobalChallenge(challengeId)
             logger.info(s"Close challenge $challengeId: ${if(closeOK) "Success" else "Error"} ")
             if (closeOK)
               watchList.remove(challengeId)
             else
               sync.wait(5000)
           case Some((challengeId, dueTime)) if dueTime > now =>
             sync.wait(dueTime - now)
           case _ => sync.wait(10000)
         }
       } catch {
         case ex =>
           error("checkExpiredChallenges", ex)
           sync.wait(5000)
       }
     }
  }

  private def notifyChanged(challenge: Challenge): Unit = {
    sync.synchronized{
      sync.notifyAll()
    }
  }

  private def closeChallenge(challengeId: Int): Boolean = Profiler(s"$clazz.closeChallenge") {

    try {
      val challenge = repository.getChallenge(challengeId).sync()
      repository.updateChallenge(challenge.copy(
        isFinished = Some(true)
      )).sync()
    }catch  {
      case _ => false
    }

  }

  private def removeFromGlobalChallenge(challengeId: Int): Boolean = Profiler(s"$clazz.removeFromGlobalChallenge") {

    try {
      repository.removeGlobalChallenge(challengeId).sync()
    }catch  {
      case _ => false
    }

  }

}
