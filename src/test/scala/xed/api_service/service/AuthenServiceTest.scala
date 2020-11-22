package xed.api_service.service

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import xed.api_service.module.XedApiModule
import xed.userprofile.AuthenService

class AuthenServiceTest extends IntegrationTest {


  override protected def injector: Injector =  Injector(Guice.createInjector(Seq(XedApiModule):_*))

  private val service = injector.instance[AuthenService]

  test("Get User By SessionId") {
//    val ssid = ""
//
//    val r = service.getUserWithSessionId(ssid).sync()
//
//    println(r)
  }


}
