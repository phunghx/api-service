package xed.api_service.util

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.{Calendar, Date, TimeZone}

import scala.collection.mutable.ListBuffer

object TimeUtils {

  def calcTimeInCurrentDay(): (Long, Long) = {
    val currentTime = Calendar.getInstance()
    currentTime.set(Calendar.HOUR_OF_DAY, 0)
    currentTime.set(Calendar.MINUTE, 0)
    currentTime.set(Calendar.SECOND, 0)
    currentTime.set(Calendar.MILLISECOND, 0)
    val start = currentTime.getTimeInMillis
    currentTime.add(Calendar.DAY_OF_MONTH, 1)
    val end = currentTime.getTimeInMillis
    (start, end)
  }

  def calcBeginOfNextDayInMills(): Long = {
    val currentTime = Calendar.getInstance()
    currentTime.set(Calendar.HOUR_OF_DAY, 0)
    currentTime.set(Calendar.MINUTE, 0)
    currentTime.set(Calendar.SECOND, 0)
    currentTime.set(Calendar.MILLISECOND, 0)
    currentTime.add(Calendar.DAY_OF_MONTH, 1)
    currentTime.getTimeInMillis
  }

  def calcBeginOfDayInMills(): (Long, String) = {
    val currentTime = Calendar.getInstance()
    currentTime.set(Calendar.HOUR_OF_DAY, 0)
    currentTime.set(Calendar.MINUTE, 0)
    currentTime.set(Calendar.SECOND, 0)
    currentTime.set(Calendar.MILLISECOND, 0)
    val start = currentTime.getTimeInMillis

    (start, f"${currentTime.get(Calendar.DAY_OF_MONTH)}%02d-${currentTime.get(Calendar.MONTH) + 1}%02d-${currentTime.get(Calendar.YEAR)}%04d")
  }

  def calcBeginOfDayInMillsFrom(mills: Long): (Long, String) = {
    val currentTime = Calendar.getInstance()
    currentTime.setTimeInMillis(mills)
    currentTime.set(Calendar.HOUR_OF_DAY, 0)
    currentTime.set(Calendar.MINUTE, 0)
    currentTime.set(Calendar.SECOND, 0)
    currentTime.set(Calendar.MILLISECOND, 0)
    val start = currentTime.getTimeInMillis

    (start, f"${currentTime.get(Calendar.DAY_OF_MONTH)}%02d-${currentTime.get(Calendar.MONTH) + 1}%02d-${currentTime.get(Calendar.YEAR)}%04d")
  }

  def getTimeStringFromMills(mills: Long): String = {
    val currentTime = Calendar.getInstance()
    currentTime.setTimeInMillis(mills)

    f"${currentTime.get(Calendar.DAY_OF_MONTH)}%02d-${currentTime.get(Calendar.MONTH) + 1}%02d-${currentTime.get(Calendar.YEAR)}%04d"
  }

  def extractTimeFromDatetime(time: Long): Long = {
    val (begin, _) = calcBeginOfDayInMillsFrom(time)
    time - begin
  }

  def parseMillsFromString(str: String, pattern: String): Long = {
    val format = new SimpleDateFormat(pattern)
    format.parse(str).getTime
  }

  def makeDayRange(from: Long, to: Long,dayInterval: Int = 1): Seq[(Long, String)] = {
    var (begin, beginStr) = calcBeginOfDayInMillsFrom(from)
    val buffer = ListBuffer.empty[(Long, String)]
    while (begin <= to) {
      if (begin >= from) {
        buffer.append((begin, beginStr))
      }
      val (time, timeStr) = calcBeginOfDayInMillsFrom(begin + TimeUnit.DAYS.toMillis(dayInterval))
      begin = time
      beginStr = timeStr
    }
    buffer.seq
  }

  def estimateNumberOfDay(duration: Long, durationOfEachStep: Long): Double = {
    //Step is 0.5
    val numStep = duration / durationOfEachStep
    val remain = duration - (numStep * durationOfEachStep)
    if (remain >= 0.6 * durationOfEachStep) { //TODO: change percent. Current 60%
      (numStep + 1).doubleValue() / 2
    } else {
      numStep.doubleValue() / 2
    }
  }



  def format(time: Long,
             pattern: Option[String] = Some("dd/MM/YYYY HH:mm:ss"),
             tz: Option[TimeZone] = None): String = {
    val f = pattern match {
      case None => new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")
      case Some(x) => new SimpleDateFormat(x)
    }
    tz match {
      case None => f.setTimeZone(TimeZone.getDefault)
      case Some(x) => f.setTimeZone(x)
    }
    f.format(new Date(time).getTime)
  }

  def parse(date: String,
            format: String,
            tz: Option[TimeZone] = Some(TimeZone.getDefault)): Long = {
    val f = new SimpleDateFormat(format)
    tz match {
      case None => f.setTimeZone(TimeZone.getDefault)
      case Some(x) => f.setTimeZone(x)
    }
    f.parse(date).getTime
  }


  def prettyTime(time: Long) = {
    val units = Array("days", "hours", "minutes", "seconds", "milliseconds")
    val metrics = Array(time / 86400000,
      (time % 86400000) / 3600000,
      ((time % 86400000) % 3600000) / 60000,
      (((time % 86400000) % 3600000) % 60000) / 1000,
      (((time % 86400000) % 3600000) % 60000) % 1000
    )

    val s = new StringBuilder()
    for (i <- metrics.indices) {
      val metric = metrics(i)
      if (metric != 0) {
        s.append(metric).append(s" ${units(i)}")
        if (i < metrics.length - 1)
          s.append(", ")
      }

    }
    s.toString
  }

}
