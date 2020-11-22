package xed.api_service.service

import java.util.concurrent.{BlockingQueue, ExecutorService, Executors, LinkedBlockingDeque}

import com.google.inject.{Inject, Singleton}
import xed.api_service.service.Operation.Operation
import xed.api_service.service.bq.{EventLog, LogService}
import xed.api_service.util.LoggerUtils
import xed.profiler.Profiler
import xed.userprofile.domain.UserProfile

case class ActionLog(action: Operation, userProfile: Option[UserProfile], data: Map[String, Any])

object Operation extends Enumeration {
  type Operation = Value
  val CREATE_DECK,
  DELETE_DECK,
  PUBLISH_DECK,
  UNPUBLISHED_DECK,
  VIEW_DECK,
  SEARCH_DECK,
  CHAT_MESSAGE,
  API_ERROR,
  ADD_TO_REVIEW,
  REVIEW,
  BOT_JOIN_CHALLENGE,
  BOT_PLAY_CHALLENGE,
  BOT_SUBMIT_CHALLENGE,
  BOT_START_REVIEW
  = Value
}

abstract class ActionLogProcessor(queue: BlockingQueue[ActionLog]) extends Runnable {
  def run() {
    while (true) {
      val item = queue.take()
      consume(item)
    }
  }

  def consume(actionLog: ActionLog)
}

trait Analytics {
  def start(): Unit

  def stop(): Unit

  def log(action: Operation, userProfile: Option[UserProfile], data: Map[String, Any]): Unit
}

@Singleton
case class AnalyticsImpl @Inject()(logService: LogService) extends Analytics {

  val queue: BlockingQueue[ActionLog] = new LinkedBlockingDeque[ActionLog]()
  val processor = AnalyticProcessor(queue, logService)
  val nThread = 8
  val pool: ExecutorService = Executors.newFixedThreadPool(nThread)

  def start(): Unit = {
    LoggerUtils.getLogger("Analytics").info("Start Analytics Service Num Thread: " + nThread)
    for (_ <- 0 to nThread) {
      pool.submit(processor)
    }
  }

  def stop(): Unit = {
    LoggerUtils.getLogger("Analytics").info("Stop Analytics Service")
    pool.shutdown()
  }

  def log(action: Operation,
          userProfile: Option[UserProfile],
          data: Map[String, Any]): Unit = Profiler("Analytics.log") {
    queue.add(ActionLog(action, userProfile, data))
  }
}


class ConsolePrinter(queue: BlockingQueue[ActionLog]) extends ActionLogProcessor(queue) {
  override def consume(actionLog: ActionLog): Unit = {
    println(actionLog)
  }
}

case class AnalyticProcessor(queue: BlockingQueue[ActionLog], logService: LogService) extends ActionLogProcessor(queue) {
  override def consume(actionLog: ActionLog): Unit = {
    logService.log(EventLog(
      eventName = actionLog.action.toString,
      source = "api-service",
      eventParams = Option(actionLog.data),
      userId = actionLog.userProfile.map(_.username).getOrElse("UNKNOWN"),
      userProperties = actionLog.userProfile
    ))
  }
}