package xed.api_service.controller

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import xed.api_service.TestServer

abstract class ControllerTest extends FeatureTest  {
  override val server = new EmbeddedHttpServer(new TestServer)

  protected val ssid = "47980490-22de-4947-bbc7-9ef0ea2f89d4"
  protected val headers = Map("Authorization" -> ssid)

}
