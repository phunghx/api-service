package xed.profiler.domain

/**
 * @author andy
 * @since 2/10/20
 **/
case class ApiProfiler(index: Int,
                       name: String,
                       totalRequest: Long,
                       pendingRequest: Long,
                       lastTime: Long,
                       highestTime: Long,
                       totalTime: Long,
                       procRate: String,
                       timeRate: String)
