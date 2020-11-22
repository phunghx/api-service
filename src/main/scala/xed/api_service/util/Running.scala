package xed.api_service.util

import java.util.{Calendar, TimeZone, Timer, TimerTask}

import com.twitter.inject.Logging
import scalaj.http.Http
import xed.api_service.util.JsonUtils._

import scala.language.postfixOps
import scala.util.control.NonFatal

/**
 * Created by phg on 2020-02-19.
 **/
trait Running extends Logging {

  private val _defaultTimer = new Timer()

  private var _stats: Map[String, Any] = Map()

  def run(delay: Long, period: Long)(func: => Unit): Unit = _run(delay, period)(func)

  def runEveryDayAt(hour: Int, minute: Int, timeZone: TimeZone, isRunNow: Boolean)(func: => Unit): Unit = {
    _runEveryDayAt(hour, minute, timeZone, isRunNow)(func)
  }

  def runEveryWeekAt(dayOfWeek: Int, hour: Int, minute: Int, timeZone: TimeZone, isRunNow: Boolean)(func: => Unit): Unit = {
    _runEveryWeekAt(dayOfWeek, hour, minute, timeZone, isRunNow)(func)
  }

  private def _runEveryWeekAt(dayOfWeek: Int, hour: Int, minute: Int, timeZone: TimeZone, isRunNow: Boolean)(func: => Unit): Unit = {
    val destDay = Calendar.getInstance(timeZone)
    destDay.set(Calendar.DAY_OF_WEEK, dayOfWeek)
    destDay.set(Calendar.HOUR_OF_DAY, hour)
    destDay.set(Calendar.MINUTE, minute)
    destDay.set(Calendar.SECOND, 0)
    _run(destDay.getTimeInMillis - System.currentTimeMillis(), 1 weekInMillis, isRunNow)(func)
  }

  private def _run(delay: Long, period: Long, isRunNow: Boolean = true)(func: => Unit): Unit = {
    var d = delay
    while (d < 0) d = d + period
    _defaultTimer.schedule(new TimerTask {
      override def run(): Unit = _exec(func)
    }, d, period)
    new Thread(new Runnable {
      override def run(): Unit = if (d > 5 * 60 * 1000 && isRunNow) _exec(func)
    }).start()
  }

  private def _exec(func: => Unit): Unit = try {
    addStats("last_run" -> System.currentTimeMillis())
    func
  } catch {
    case NonFatal(throwable) =>
      addStats("last_exception_at" -> System.currentTimeMillis())
      addStats("last_exception" -> Map(
        "localized_message" -> throwable.getLocalizedMessage,
        "stack_trace" -> throwable.getStackTrace.map(_.toString)
      ))
      error(s"[ERROR]Running.run", throwable)
  }

  protected def addStats(map: (String, Any)): Unit = _stats = _stats + map

  def runWithNewTimer(delay: Long, period: Long)(func: => Unit): Timer = {
    val timer = new Timer()
    timer.schedule(new TimerTask {
      override def run(): Unit = func
    }, delay, period)
    timer
  }

  def terminate(): Unit = {
    _defaultTimer.cancel()
    _defaultTimer.purge()
  }

  def statistics: Map[String, Any] = _stats

  def runEveryDayForEachTimeZone(hour: Int, minute: Int)(func: TimeZone => Unit): Unit = {
    val timeZones = allTimeZone()
    _runEveryDayAt(hour, minute, timeZones.head, isRunNow = false) {
      _exec(timeZones)(func)
    }
  }

  private def _runEveryDayAt(hour: Int, minute: Int, timeZone: TimeZone, isRunNow: Boolean)(func: => Unit): Unit = {
    info(s"_runEveryDayAt $hour:$minute - ${timeZone.getDisplayName}")
    val destDay = Calendar.getInstance(timeZone)
    destDay.set(Calendar.HOUR_OF_DAY, hour)
    destDay.set(Calendar.MINUTE, minute)
    destDay.set(Calendar.SECOND, 0)
    _run(destDay.getTimeInMillis - System.currentTimeMillis(), 1 dayInMillis, isRunNow)(func)
  }

  implicit class LongTimeLike(long: Long) {

    def weekInMillis: Long = (long * 7) dayInMillis

    def dayInMillis: Long = (long * 24) hourInMillis

    def hourInMillis: Long = (long * 60) minuteInMillis

    def minuteInMillis: Long = (long * 60) secondInMillis

    def secondInMillis: Long = long * 1000

    def dayBeforeInMillis: Long = System.currentTimeMillis() - 2 dayInMillis
  }

  def allTimeZone(): Array[TimeZone] = {
    var map: Map[Int, TimeZone] = Map()
    TimeZone.getAvailableIDs.foreach(id => {
      val tz = TimeZone.getTimeZone(id)
      map += (tz.getRawOffset -> tz)
    })
    map.values.toArray.sortWith((f1, f2) => f1.getRawOffset > f2.getRawOffset)
  }

  private def _exec(timeZones: Array[TimeZone])(func: TimeZone => Unit): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        timeZones.zipWithIndex.foreach(f => {
          val offset = f._1.getRawOffset
          val index = f._2

          func(f._1)

          if (index < timeZones.length - 1) {
            Thread.sleep(Math.abs(timeZones(index + 1).getRawOffset - offset))
          }
        })
      }
    }).start()
  }

  def runEveryWeekForEachTimeZone(dayOfWeek: Int, hour: Int, minute: Int)(func: TimeZone => Unit): Unit = {
    val timeZones = allTimeZone()
    _runEveryWeekAt(dayOfWeek, hour, minute, timeZones.head, isRunNow = false) {
      _exec(timeZones)(func)
    }
  }

  protected def addStats(map: Map[String, Any]): Unit = _stats = _stats ++ map

  protected def log(message: String): Unit = {
    info(message)
    val res = Http(s"https://api.telegram.org/bot949300298:AAH936hGWcWCAeNcs8k8_WGtjDsXGFu0CEY")
      .method("POST")
      .header("Content-Type", "application/json")
      .postData(SendMessage(
        chatId = "-349249185",
        text = message,
        parseMode = Some("HTML")
      ).toJsonString)
      .asString
    info(res.body)
  }

  case class SendMessage(
    chatId: String,
    text: String,
    parseMode: Option[String] = None,
    disableWebPagePreView: Option[Boolean] = None
  )
}
