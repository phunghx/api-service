package adhoc

import java.util.TimeZone

import xed.worker.{TimeHelper, WeeklyReportHelper}

/**
 * Created by phg on 2020-03-13.
 **/
object TestTime extends TimeHelper {
  def main(args: Array[String]): Unit = {
    val map = WeeklyReportHelper(TimeZone.getTimeZone("GMT+7")).getReviewsLastWeek
    println(map.map(f => s"${f._1} -> ${f._2}").mkString("\n"))
  }
}

