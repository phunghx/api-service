package xed.api_service.util

/**
 * Created by phg on 2020-02-13.
 **/
object ThreadUtil {
  implicit class ThreadLike(thread: Thread) {
    def getMethodName: String = {
      val stackTraces = thread.getStackTrace
      if (stackTraces.size >= 2) stackTraces(2).getMethodName
      else ""
    }
  }
}
