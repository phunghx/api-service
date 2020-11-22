package com.twitter.finatra.http.modules

import com.github.mustachejava.{DefaultMustacheFactory, Mustache, MustacheFactory}
import com.google.inject.Provides
import com.twitter.finatra.XedMessageBodyModule
import com.twitter.finatra.http.internal.marshalling.mustache.ScalaObjectHandler
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag
import javax.inject.Singleton

object OverridedMustacheModule extends TwitterModule {

  private val templatesDir =
    flag("mustache.templates.dir", "templates", "templates resource directory")

  override def modules = Seq(DocRootModule, XedMessageBodyModule)

  @Provides
  @Singleton
  def provideMustacheFactory(resolver: FileResolver, @Flag("local.doc.root") localDocRoot: String): MustacheFactory = {
    // templates are cached only if there is no local.doc.root
    val cacheMustacheTemplates = localDocRoot.isEmpty
    val templatesDirectory = templatesDir()

    new DefaultMustacheFactory(templatesDirectory) {
      setObjectHandler(new ScalaObjectHandler)

      override def compile(name: String): Mustache = {
        if (cacheMustacheTemplates) {
          super.compile(name)
        } else {
          new LocalFilesystemDefaultMustacheFactory(templatesDirectory, resolver).compile(name)
        }
      }
    }
  }
}
