package xed.api_service.util

import java.util.Calendar

import com.twitter.inject.Logging


object Scheduler {
  val Hourly = 1
  val Daily = 2
  val Monthly = 3
  val Weekly = 4
}

abstract class Scheduler extends Thread with Logging {
  def diffToNextCheckpoint(): Long

  def exec()

  final override def run(): Unit = {
    while (true) {
      try {
        val sleepTime = diffToNextCheckpoint()
        nextCheckpoint(sleepTime)
        exec()
      } catch {
        case ex: Throwable => logger.error("run", ex)
      }
    }
  }

  private def nextCheckpoint(time: Long) = {
    if (time > 0) {
      logger.info(s"Waiting for ${TimeUtils.prettyTime(time)}")
      Thread.sleep(time)
    }
  }
}

abstract class HourlyScheduler(val minutes:Int = 0) extends Scheduler {

  override def diffToNextCheckpoint(): Long = {
    val cal = Calendar.getInstance()
    cal.set(cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.HOUR_OF_DAY),
      minutes,0
    )
    cal.set(Calendar.MILLISECOND,0)

    val currentTime = System.currentTimeMillis()
    if(currentTime > cal.getTimeInMillis) {
      cal.add(Calendar.HOUR_OF_DAY,1)
    }

    cal.getTimeInMillis - currentTime
  }
}

abstract class DailyScheduler(val hours: Int,
                              val minutes:Int = 0) extends Scheduler {

  override def diffToNextCheckpoint(): Long = {
    val cal = Calendar.getInstance()
    cal.set(cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
      hours,minutes,0
    )
    cal.set(Calendar.MILLISECOND,0)

    val currentTime = System.currentTimeMillis()
    if(currentTime > cal.getTimeInMillis) {
      cal.add(Calendar.DAY_OF_MONTH,1)
    }

    cal.getTimeInMillis - currentTime
  }
}

abstract class MonthlyScheduler(val days: Int,
                                val hours: Int,
                                val minutes:Int = 0) extends Scheduler {

  override def diffToNextCheckpoint(): Long = {
    val cal = Calendar.getInstance()
    cal.set(cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      days,
      hours,minutes,0
    )
    cal.set(Calendar.MILLISECOND,0)

    val currentTime = System.currentTimeMillis()
    if(currentTime > cal.getTimeInMillis) {
      cal.add(Calendar.MONTH,1)
    }

    cal.getTimeInMillis - currentTime
  }
}

abstract class WeeklyScheduler(val weekDay: Int,
                               val hours: Int,
                               val minutes:Int = 0) extends Scheduler {

  override def diffToNextCheckpoint(): Long = {
    val cal = Calendar.getInstance()
    cal.set(cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
      hours,minutes,0
    )
    cal.set(Calendar.MILLISECOND,0)
    cal.set(Calendar.DAY_OF_WEEK,weekDay)

    val currentTime = System.currentTimeMillis()
    if(currentTime > cal.getTimeInMillis) {
      cal.add(Calendar.WEEK_OF_YEAR,1)
    }

    cal.getTimeInMillis - currentTime
  }
}

