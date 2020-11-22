package xed.profiler

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicLong, LongAdder}

import com.twitter.util.Future
import xed.profiler.domain.{ApiProfiler, ProfilerViewData}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * @author anhlt
  */
case class ProfilerValue() {
  private val _totalTime = new LongAdder()
  private val _pendingHit = new LongAdder()
  private val _hit = new LongAdder()
  private val _lastTime = new AtomicLong()
  private val _highestTime = new AtomicLong()

  def reset(): Unit = {
    _totalTime.reset()
    _pendingHit.reset()
    _hit.reset()
  }

  def lastTime = _lastTime.longValue()

  def lastTime_=(elapseTime: Long) = {
    _lastTime.set(elapseTime)
  }

  def totalTime = _totalTime.longValue()

  def pendingHit = _pendingHit.longValue()

  def hit = _hit.longValue()

  def highestTime = _highestTime.longValue()

  def incrTotalTime(elapseTime: Long) = {
    _totalTime.add(elapseTime)
  }

  def incrHit = _hit.increment()

  def incrHit(step: Int) = _hit.add(step)

  def incrPending = _pendingHit.increment()

  def decrPending = _pendingHit.decrement()

  def updateHighestTime(time: Long) = {
    if (_highestTime.longValue() < time) {
      _highestTime.set(time)
    }
  }

  override def equals(that: Any): Boolean = {
    if (that == null || !that.isInstanceOf[ProfilerValue]) false
    else {
      val obj = that.asInstanceOf[ProfilerValue]
      obj._totalTime.longValue() == _totalTime.longValue() &&
      obj._pendingHit.longValue() == _pendingHit.longValue() &&
      obj._hit.longValue() == _hit.longValue() &&
      obj._lastTime.longValue() == _lastTime.longValue()
    }
  }
}

trait ProfilerRepository {

  def profiles: Map[String, ProfilerValue]

  def push(key: String)

  def pop(key: String, elapseTime: Long)

  def pop(key: String, elapseTime: Long, step: Int)

  def put(key: String, profilerValue: ProfilerValue)

  def clearAll()

  def reset(key: String)
}

case class InMemoryProfilerRepository() extends ProfilerRepository {

  private val mapProfiler = new ConcurrentHashMap[String, ProfilerValue]()

  override def push(key: String): Unit = {
    var value = mapProfiler.get(key)
    if (value != null) {
      value.incrPending
    } else {
      mapProfiler.synchronized {
        value = ProfilerValue()
        value.incrPending
        mapProfiler.put(key, value)
      }
    }
  }

  override def pop(key: String, elapseTime: Long): Unit = {
    val value = mapProfiler.get(key)
    if (value != null) {
      value.decrPending
      value.incrHit
      value.lastTime = elapseTime
      value.incrTotalTime(elapseTime)
      value.updateHighestTime(elapseTime)
    }
  }

  override def pop(key: String, elapseTime: Long, step: Int): Unit = {
    val value = mapProfiler.get(key)
    if (value != null) {
      value.decrPending
      value.incrHit(step)
      value.lastTime = elapseTime
      value.incrTotalTime(elapseTime)
      value.updateHighestTime(elapseTime)
    }
  }

  override def profiles = mapProfiler.asScala.toMap[String, ProfilerValue]

  override def put(key: String, value: ProfilerValue): Unit = {
    mapProfiler.put(key, value)
  }

  override def clearAll(): Unit = mapProfiler.clear()

  override def reset(key: String): Unit = {
    mapProfiler.get(key).reset()
  }
}

trait Profiler {

  /**
    * profile a given `f`
    */
  def apply[A](f: => A): A

  /**
    * profile a given asynchronous `f`
    */
  def apply[A](f: => Future[A]): Future[A]
}

object Profiler {
  private val profilerRepo = InMemoryProfilerRepository()

  private var today = LocalDate.now

  private def isNewDay: Boolean = !today.isEqual(LocalDate.now)

  private val mapFn: mutable.Map[String, Profiler] =
    new ConcurrentHashMap[String, Profiler]().asScala

  def init(profilingData: Map[String, ProfilerValue]): Unit = {
    profilerRepo.synchronized({
      profilingData.foreach(f => profilerRepo.put(f._1, f._2))
    })
  }

  def apply(key: String, step: Int = 1): Profiler = {

    if (!mapFn.contains(key)) {
      mapFn.synchronized({
        if (!mapFn.contains(key)) {
          mapFn.put(key, new Profiler {
            override def apply[A](f: => A): A = {
              profilerRepo.push(key)
              val startTime = System.nanoTime
              try {
                f
              } finally {
                val elapseTime = (System.nanoTime - startTime) / 1000000
                profilerRepo.pop(key, elapseTime, step)
              }
            }

            override def apply[A](f: => Future[A]): Future[A] = {
              val startTime = System.nanoTime
              profilerRepo.push(key)
              f.ensure({
                val elapseTime = (System.nanoTime - startTime) / 1000000
                profilerRepo.pop(key, elapseTime, step)
              })
            }
          })
        }

      })
    }
    if (isNewDay) {
      today = LocalDate.now
    }
    mapFn(key)
  }

  def clearAll(): Unit = {
    profilerRepo.clearAll()
  }

  def getProfilerViewData: ProfilerViewData = {
    val apiProfilers = profilerRepo.profiles.keys
      .toSeq
      .sortWith(_.compareTo(_) < 0)
      .zipWithIndex.map { case (name, index) =>
      val data = profilerRepo.profiles(name)
      ApiProfiler(
        index = index,
        name = name,
        totalRequest = data.hit,
        pendingRequest = data.pendingHit,
        lastTime = data.lastTime,
        highestTime = data.highestTime,
        totalTime = data.totalTime,
        procRate = f"${if (data.totalTime != 0) data.hit * 1000.0 / data.totalTime else 0}%1.2f",
        timeRate = f"${if (data.hit != 0) data.totalTime * 1.0 / data.hit else 0}%1.2f"
      )
    }

    ProfilerViewData(apiProfilers, Seq.empty)

  }

}
