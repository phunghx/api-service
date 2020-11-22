package xed.api_service.service

import com.google.inject.Guice
import com.twitter.inject.{Injector, IntegrationTest}
import xed.api_service.module.XedApiModule
import xed.api_service.service.Operation._
import xed.userprofile.domain.UserProfile

class AnalyticsTest extends IntegrationTest {
  private val analytics = injector.instance[Analytics]

  override def beforeAll() {
    analytics.start()
  }

  override def afterAll() {
    analytics.stop()
  }

  def fakeUserProfile() = Some(UserProfile(s"from_test_${System.currentTimeMillis()}", None, None, None, None, None, None, None))

  test("Test ConsolePrinterProcessor") {

    for (i <- 0 to 10) {
      analytics.log(CREATE_DECK, fakeUserProfile(), Map(
        "current_millis" -> System.currentTimeMillis()
      ))
    }

    while (true) {}
  }

  override protected def injector: Injector = Injector(Guice.createInjector(Seq(XedApiModule): _*))
}


