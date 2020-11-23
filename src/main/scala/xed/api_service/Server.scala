package xed.api_service

import com.google.inject.Module
import com.google.inject.util.Modules
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.modules.OverridedMustacheModule
import com.twitter.finatra.http.routing.HttpRouter
import xed.api_service.Server._
import xed.api_service.controller.http._
import xed.api_service.controller.http.filter.{InjectAuthenFilter, LoggedInUserFilter}
import xed.api_service.module._
import xed.api_service.service.Analytics
import xed.api_service.util.ZConfig
import xed.profiler.controller.http.ProfilerController

object MainApp extends Server

class TestServer extends  Server {

  override def modules: Seq[com.google.inject.Module] = Seq(overrideModule(super.modules ++ Seq(XedApiModuleTestImpl): _*))

}

object Server {
  def overrideModule(modules: Module*): Module = {
    if (modules.size == 1) return modules.head

    var module = modules.head
    modules.tail.foreach(m => {
      module = Modules.`override`(module).`with`(m)
    })
    module
  }
}

class Server extends HttpServer {
  System.setProperty("es.set.netty.runtime.available.processors", "false")
  override protected def defaultFinatraHttpPort: String = ZConfig.getString("server.http.port", ":8080")

  override protected def disableAdminHttpServer: Boolean = ZConfig.getBoolean("server.admin.disable", true)

  override def modules: Seq[Module] = Seq(XedApiModule,
    PublicPathConfigModule)

  override def messageBodyModule = com.twitter.finatra.XedMessageBodyModule

  override protected def mustacheModule: Module = OverridedMustacheModule

  override protected def configureHttp(router: HttpRouter): Unit = {
    router.filter[filter.CORSFilter](beforeRouting = true)
      .filter[CommonFilters]
      .filter[InjectAuthenFilter]
      .add[ProfilerController]
      .add[TestWriterController]
      .add[DeckController]
      .add[LoggedInUserFilter,CardController]
      .add[LoggedInUserFilter,ReviewController]
      .add[LoggedInUserFilter,StatisticController]
      .add[Resources]


      .exceptionMapper[filter.CaseClassExceptionMapping]
      .exceptionMapper[filter.JsonParseExceptionMapping]
      .exceptionMapper[filter.CommonExceptionMapping]

  }

  override def afterPostWarmup(): Unit = {
    super.afterPostWarmup()
    info("afterPostWarmup")

    injector.instance[Analytics].start()
  }
}
