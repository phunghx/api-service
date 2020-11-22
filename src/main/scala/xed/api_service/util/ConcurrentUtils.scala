package xed.api_service.util

import com.twitter.util.Future

object ConcurrentUtils {
  def getUntilNone[T](
                       b: Seq[T],
                       head: Future[Option[T]],
                       hasNext: T => Boolean,
                       getNext: T => Future[Option[T]]
                     ): Future[Seq[T]] = {
    head.flatMap {
      case Some(t) if hasNext(t) => getUntilNone(b :+ t, getNext(t), hasNext, getNext)
      case Some(t) => Future.value(b :+ t)
      case None => Future.value(b)
    }
  }


  def getUntilEmpty[T](
                        b: Seq[T],
                        getFn: (Int, Int) => Future[Seq[T]],
                        from: Int,
                        size: Int
                      ): Future[Seq[T]] = {
    getFn(from, size).flatMap(r =>
      if (r.length < size) Future.value(b ++ r)
      else getUntilEmpty(b ++ r, getFn, from + size, size)
    )
  }

  def processUntilDefined[B, T](seq: Seq[T], z: Future[Option[B]], op: T => Future[Option[B]]): Future[Option[B]] = {
    z.flatMap {
      case x if x.isDefined => Future.value(x)
      case _ =>
        if (seq.isEmpty) {
          Future.value(None)
        } else {
          processUntilDefined(seq.tail, op(seq.head), op)
        }
    }
  }
}
