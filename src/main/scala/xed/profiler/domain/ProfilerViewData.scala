package xed.profiler.domain

/**
 * @author andy
 * @since 2/10/20
 **/
case class ProfilerViewData(profilers: Seq[ApiProfiler],
                            dailyProfilers: Seq[ApiProfiler])
