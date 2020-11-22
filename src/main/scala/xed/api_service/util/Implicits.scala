package xed.api_service.util

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar, TimeZone}

import com.google.protobuf.util.JsonFormat
import com.google.protobuf.{ListValue, Struct, Value}
import com.twitter.util.{Await, Future, FuturePool, Promise => TwitterPromise}
import xed.api_service.domain.course.JourneyInfo
import xed.api_service.domain.design.v100.{MultiChoice, MultiSelect}
import xed.api_service.domain.request.SearchRequest
import xed.api_service.domain.{Card, Deck}

import scala.concurrent.{Future => ScalaFuture}
import scala.io.Source
/**
  * @author anhlt
  */
object Implicits {


  implicit val futurePool = FuturePool.unboundedPool

  implicit def async[A](f: => A): Future[A] = futurePool { f }

  def tryWith[A <: AutoCloseable, B](a: A)(fn: A => B): B = {
    try {
      fn(a)
    } finally {
      if (a != null) {
        a.close()
      }
    }
  }

  def usingSource[A <: Source, B](a: A)(fn: A => B): B = {
    try {
      fn(a)
    } finally {
      if (a != null) {
        a.close()
      }
    }
  }

  implicit class FutureEnhance[T](f: Future[T]) {
    def sync(): T = Await.result(f)
  }

  implicit def value2Opt[A](f: A): Option[A] = Option(f)

  implicit def opt2Value[A](f: Option[A]): A = f.get




  implicit class ImplicitString(value: String) {
    def asUserAvatar = ZConfig.getString("user.avatar_url_template").replaceAll("\\$userId", value)

    def asJsonNode = JsonUtils.readTree(value)

    def asOption: Option[String] = {
      Option(value)
        .filter(_.nonEmpty)
        .flatMap(x => if (x == null || x.trim.isEmpty) None else Some(x.trim))
    }

    def orValue(v: String) = asOption.getOrElse(v)

    def fromJson[T: Manifest](): T = JsonUtils.fromJson[T](value)

    def encodeURI = Utils.encodeURIComponent(value)

    def splitSeq(separator: String = ",", limit: Int = 0): Seq[String] = value.split(separator, limit).filterNot(_.isEmpty).toSeq

    def makeId: String = {
      value
        .asOption
        .map(_.toLowerCase)
        .map(x => {
          x.replaceAll("\\s+", "_")
            .replaceAll("[^\\w]+", "_")
            .replaceAll("[\\n|\\r]+", "_")
            .replaceAll("_+", "_")
        })
    }

  }

  implicit class ImplicitOptString(value: Option[String]) {
    def ignoreEmpty: Option[String] = value.filter(_.nonEmpty)
  }

  implicit class ImplicitBoolean(value: Option[Boolean]) {
    def orFalse: Boolean = value.getOrElse(false)

    def orTrue: Boolean = value.getOrElse(true)
  }

  implicit class ImplicitAny(value: Any) {
    def toJson(pretty: Boolean = false) = JsonUtils.toJson(value, pretty)

    def asOptInt: Option[Int] = value.asOptAny.map(_.asInstanceOf[Int])

    def asOptLong: Option[Long] = value.asOptAny.map(_.asInstanceOf[Long])

    def asOptString: Option[String] = value.asOptAny.map(_.asInstanceOf[String])

    def asOptDouble: Option[Double] = value.asOptAny.map(_.asInstanceOf[Double])

    def asOptFloat: Option[Float] = value.asOptAny.map(_.asInstanceOf[Float])

    def asOptBoolean: Option[Boolean] = value.asOptAny.map(_.asInstanceOf[Boolean])

    def asOptShort: Option[Short] = value.asOptAny.map(_.asInstanceOf[Short])

    def asOptAny: Option[Any] = value match {
      case s: Option[_] => s
      case _ => Option(value)
    }

    def orEmpty: String = value match {
      case Some(x) => x.toString
      case _ => value.toString
    }

    def toMapAny: Map[String, Any] = {

      JsonUtils.fromJson[Map[String, Any]](JsonUtils.toJson(value, false))
    }

    def toHttpGetParam: String = {
      import scala.collection.JavaConversions._
      JsonUtils.readTree(JsonUtils.toJson(value, false))
        .fields()
        .toSeq
        .map(f => s"${f.getKey}=${Utils.encodeURIComponent(f.getValue.asText(""))}")
        .mkString("&")
    }
  }

  implicit class ImplicitJourney(journeyInfo: JourneyInfo) {

    def value() = {
      val builder = Struct.newBuilder()
      JsonFormat.parser().merge(JsonUtils.toJson(journeyInfo), builder)
      Value.newBuilder()
        .setStructValue(builder.build())
        .build()
    }
  }


  implicit class ImplicitDeck(deck: Deck) {

    def value() = {
      val builder = Struct.newBuilder()
      JsonFormat.parser().merge(JsonUtils.toJson(deck), builder)
      Value.newBuilder()
        .setStructValue(builder.build())
        .build()
    }
  }

  implicit class ImplicitCardList(items: Seq[Card]) {

    def listValue() = {
      items.map(item =>{
        val builder = Struct.newBuilder()
        JsonFormat.parser().merge(JsonUtils.toJson(item), builder)
        Value.newBuilder()
          .setStructValue(builder.build())
          .build()
      }).foldLeft(ListValue.newBuilder())((builder,v) => builder.addValues(v))
        .build()
    }
  }

  implicit class ImplicitStringList(items: Seq[String]) {

    def listValue() = {
      items.map(id =>{
        Value.newBuilder()
          .setStringValue(id)
          .build()
      }).foldLeft(ListValue.newBuilder())((builder,v) => builder.addValues(v))
        .build()
    }
  }

  implicit class ImplicitIntList(items: Seq[Int]) {

    def listValue() = {
      items.map(id =>{
        Value.newBuilder()
          .setNumberValue(id)
          .build()
      }).foldLeft(ListValue.newBuilder())((builder,v) => builder.addValues(v))
        .build()
    }
  }

  implicit class ImplicitList[T](value: Seq[T]) {

    def itemIsDuplicated: Boolean = value.distinct.size != value.size

    def itemDuplicated: Seq[T] = {
      value.groupBy(f => f).collect {
        case (x, ys) if ys.size > 1 => x
      }.toSeq
    }

    def opt: Option[Seq[T]] = Option(value) match {
      case Some(x) if x.isEmpty => None
      case x => x
    }

  }

  implicit class ImplicitCollection[A](value: Stream[A]) {
    def convertToSeq: Seq[A] = {
      val seq = scala.collection.mutable.ListBuffer.empty[A]
      val it = value.iterator
      while (it.hasNext) seq += it.next()
      seq
    }
  }

  implicit class ImplicitMilliseconds(value: Long) {
    def asDayOfWeek: Int = {
      val calendar = new GregorianCalendar()
      calendar.setTime(new Date(value))
      calendar.get(Calendar.DAY_OF_WEEK)
    }

    def asDayOfMonth: Int = {
      val calendar = new GregorianCalendar()
      calendar.setTime(new Date(value))
      calendar.get(Calendar.DAY_OF_MONTH)
    }

    def asDate: Date = {
      new Date(value)
    }

    def format(formatted: String = "yyyy-MM-dd"): String = {
      new SimpleDateFormat(formatted).format(value)
    }

    def asTimeFormula(): String = {
      value match {
        case x if x > 0 => {
          val calendar = new GregorianCalendar()
          calendar.setTime(new Date(value))
          calendar.setTimeZone(TimeZone.getDefault)
          s"TIME(${calendar.get(Calendar.HOUR_OF_DAY)}, ${calendar.get(Calendar.MINUTE)}, ${calendar.get(Calendar.SECOND)})"
        }
        case _ => {
          s"TIME(0,0,0)"
        }
      }
    }

    def asTimeFormulaWithoutTimeZone(): String = {
      val hours = value / 3600000
      var tmp = (value % 3600000)
      val minutes = tmp / 60000
      tmp = tmp % 60000
      val seconds = tmp / 1000
      s"TIME(${hours}, ${minutes}, ${seconds})"
    }
  }

  implicit class ImplicitDayOfWeek(value: Int) {
    def asString: String = value match {
      case Calendar.MONDAY => "Mon"
      case Calendar.TUESDAY => "Tue"
      case Calendar.WEDNESDAY => "Wed"
      case Calendar.THURSDAY => "Thu"
      case Calendar.FRIDAY => "Fri"
      case Calendar.SATURDAY => "Sat"
      case Calendar.SUNDAY => "Sun"
      case _ => s"unknown"
    }
  }

  implicit class ImplicitSearchRequest(request: SearchRequest) {
    def hasCategoryFilter: Boolean = {
      request.terms.exists(_.exists(_.field.equalsIgnoreCase("category")))
    }
  }

  implicit class ImplicitMultiSelectToMultiChoice(component: MultiSelect) {
    def asMultiChoice() = {
      MultiChoice(
        question = component.question,
        textConfig = component.textConfig,
        answers = component.answers
      )
    }
  }

  import language.implicitConversions
  import scala.concurrent.ExecutionContext
  import scala.util.{Failure, Success}

  implicit class ScalaFutureLike[A](val sf: ScalaFuture[A]) extends AnyVal {
    def asTwitter(implicit e: ExecutionContext): Future[A] = {
      val promise: TwitterPromise[A] = new TwitterPromise[A]()
      sf.onComplete {
        case Success(value)     => promise.setValue(value)
        case Failure(exception) => promise.setException(exception)
      }
      promise
    }
  }


}
