package com.twitter.finatra
import com.twitter.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.http.internal.marshalling.{DefaultMessageBodyReaderImpl, MessageBodyManager}
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter}
import com.twitter.finatra.response.Mustache
import com.twitter.inject.{Injector, InjectorModule, TwitterModule}
import xed.api_service.module.XedResponseWriterImpl

object XedMessageBodyModule extends TwitterModule {

  flag("http.response.charset.enabled", true, "Return HTTP Response Content-Type UTF-8 Charset")

  override val modules = Seq(InjectorModule)

  protected override def configure(): Unit = {
    bindSingleton[DefaultMessageBodyReader].to[DefaultMessageBodyReaderImpl]
    bindSingleton[DefaultMessageBodyWriter].to[XedResponseWriterImpl]
  }

  override def singletonStartup(injector: Injector) {
    val manager = injector.instance[MessageBodyManager]
    manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]
    manager.addByComponentType[MustacheBodyComponent, MustacheMessageBodyWriter]

  }
}
