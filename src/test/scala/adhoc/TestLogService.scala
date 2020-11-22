package adhoc

import xed.api_service.service.bq.{EventLog, HttpLogService, LogService}
import xed.api_service.util.ZConfig
import xed.userprofile.domain.UserProfile

/**
 * Created by phg on 2020-02-11.
 **/
object TestLogService {
//  private val injector = Injector(Guice.createInjector(XedApiModule))
//  private val logService = injector.instance[LogService]
  val logService: LogService = HttpLogService(ZConfig.getConf("remote-logger"))
  def main(args: Array[String]): Unit = {
    logService.log(EventLog(
      eventName = "test-from-api-service",
      eventParams = Option(Map(
        "level_1" -> Map(
          "level_2" -> Map(
            "level_3" -> Map(
              "level_4" -> "value-x"
            )
          )
        )
      )),
      source = "api-service-test",
      userId = "pseudo-blah-blah-blah",
      userProperties = Option(UserProfile(
        "blahblahblah"
      ))
    ))

//    while (true) {}
  }
}
