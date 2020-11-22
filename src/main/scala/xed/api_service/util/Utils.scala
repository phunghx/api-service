package xed.api_service.util

import java.io._
import java.net.{NetworkInterface, URLEncoder}
import java.text.{Normalizer, SimpleDateFormat}
import java.util.{Calendar, Date, TimeZone, UUID}

import com.google.inject.Module
import com.google.inject.util.Modules
import xed.api_service.domain.exception.NotFoundError

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * @author anhlt
  */
object Utils {
  def randomString = {
    UUID.randomUUID().toString.replaceAll("-", "")
  }


  val TIME_ZONE_ASIA_HCM: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

  val DEFAULT_DATE_FORMAT_UTC: SimpleDateFormat = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf
  }

  def getTimeInMs(time: String, format: String = "yyyy-MM-dd HH:mm:ss"): Long = {
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.parse("1970-01-01 " + time).getTime
  }

  def getHourBeforeInMs(hourBefore: Int): Long = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR, -hourBefore)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def getDayBeforeInMs(dayBefore: Int): Long = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, -dayBefore)
    cal.set(Calendar.HOUR, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTimeInMillis
  }

  def writeTextFile(file: File, text: String): Unit = {
    var bw: BufferedWriter = null
    try {
      bw = new BufferedWriter(new FileWriter(file))
      bw.write(text)
    } finally {
      if (bw != null) bw.close()
    }
  }

  def writeTextFile(fileName: String, text: String): Unit = {
    var bw: BufferedWriter = null
    try {
      bw = new BufferedWriter(new FileWriter(new File(fileName)))
      bw.write(text)
    } finally {
      if (bw != null) bw.close()
    }
  }

  def timeToString(time: Long, pattern: String, tz: TimeZone = TimeZone.getDefault): String = {
    dateToString(new Date(time), pattern, tz)
  }

  def dateToString(date: Date, pattern: String, tz: TimeZone = TimeZone.getDefault): String = {
    val sdf = new SimpleDateFormat(pattern)
    sdf.setTimeZone(tz)
    sdf.format(date)
  }

  def stringToTime(dateTime: String, format: String, tz: TimeZone = TimeZone.getDefault): Long = {
    stringToDate(dateTime, format, tz).getTime
  }

  def stringToDate(dateTime: String, format: String, tz: TimeZone = TimeZone.getDefault): Date = {
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(tz)
    sdf.parse(dateTime)
  }

  def getStackTraceAsString(t: Throwable) = {
    val sw = new StringWriter
    t.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  def parseCookie(cookies: String): Map[String, String] = {
    cookies.split(";").flatMap(f => {
      val splits = f.split("=")
      if (splits.length == 2) Some(splits(0).trim() -> splits(1).trim()) else None
    }).toMap[String, String]
  }


  def getEvn(name: String): Option[String] = {
    Option(System.getProperty(name)) match {
      case Some(x) => Some(x)
      case _ => Option(System.getenv(name))
    }
  }

  def getIpAddress(): Seq[String] = {
    val list = scala.collection.mutable.ListBuffer.empty[String]
    val networks = NetworkInterface.getNetworkInterfaces
    while (networks.hasMoreElements) {
      val network = networks.nextElement()
      if (!network.isLoopback && !network.isVirtual && network.isUp) {
        val addresses = networks.nextElement().getInetAddresses
        while (addresses.hasMoreElements) {
          val address = addresses.nextElement()
          list += address.getHostAddress
        }
      }
    }
    list
  }

  def printQuery(query: String): Unit = {
    println()
    println("==============================QUERY=================================")
    println(query)
    println("====================================================================")
    println()
  }


  def normalizeEmail(email: String): String = {
    email.replace("@", "_at_")
      .replaceAll("""[^\w]""", "-")
  }


  def encodeURIComponent(s: String): String = {
    URLEncoder.encode(s, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }

  def getValue[T](value: Option[T], isEmpty: T => Boolean, fWhenEmpty: => Unit): Option[T] = {
    if (value.isDefined && isEmpty(value.get)) {
      fWhenEmpty
      None
    } else value
  }

  def unquote(s: String): String =
    if (s.isEmpty) s else s(0) match {
      case '"' => unquote(s.tail)
      case '\\' => s(1) match {
        case 'b' => '\b' + unquote(s.drop(2))
        case 'f' => '\f' + unquote(s.drop(2))
        case 'n' => '\n' + unquote(s.drop(2))
        case 'r' => '\r' + unquote(s.drop(2))
        case 't' => '\t' + unquote(s.drop(2))
        case '"' =>
          '"' + unquote(s.drop(2))
        case 'u' => Integer.parseInt(s.slice(2, 6), 16).toChar + unquote(s.drop(6))
        case c => c + unquote(s.drop(2))
      }
      case c => c + unquote(s.tail)
    }

  def overrideModule(modules: Module*): Module = {
    if (modules.size == 1) return modules.head

    var module = modules.head
    modules.tail.foreach(m => {
      module = Modules.`override`(module).`with`(m)
    })
    module
  }

  def getFullStacktrace(t: Throwable): String = {
    val printer = new StringWriter()
    t.printStackTrace(new PrintWriter(printer))
    printer.toString
  }


  def throwIfNotExist[T](v: Option[T], msg: Option[String] = None) = v match {
    case Some(x) => x
    case _ => throw NotFoundError(msg)
  }

  def getRandomSubset[T](items: Seq[T]): Seq[T] = {
    val rand = new Random()
    val srcList = ListBuffer(items:_*)
    val newList = ListBuffer.empty[T]
    while(srcList.nonEmpty) {
      val randomIndex = rand.nextInt(srcList.size)
      newList.append(srcList(randomIndex))
      srcList.remove(randomIndex)
    }
    newList
  }

  def getRandomSubset[T](items: Seq[T], num: Int): Seq[T] = {
    val rand = new Random()
    val srcList = ListBuffer(items:_*)
    val newList = ListBuffer.empty[T]
    while(newList.length < num && srcList.nonEmpty) {
      val randomIndex = rand.nextInt(srcList.size)
      newList.append(srcList(randomIndex))
      srcList.remove(randomIndex)
    }
    newList
  }


  def isUnicodeStrEquals(s1: String, s2: String)= {
    val normalizedS1 = Normalizer.normalize(s1, Normalizer.Form.NFD)
      .replaceAll("['’]","'")
      .replaceAll("[“\"]","\"")
      .trim
    val normalizedS2 = Normalizer.normalize(s2, Normalizer.Form.NFD)
      .replaceAll("['’]","'")
      .replaceAll("[“\"]","\"")
      .trim

    normalizedS1.equalsIgnoreCase(normalizedS2)
  }
}
