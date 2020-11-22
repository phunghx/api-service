package xed.api_service.module

import com.google.inject.{Provides, Singleton}
import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.TwitterModule
import xed.api_service.util.ZConfig

object PublicPathConfigModule extends TwitterModule {

  val keyFileDoc = flag("local.doc.root", ZConfig.getLocalDocRoot(), "The key to use.") // file path
  val keyDoc = flag("doc.root", ZConfig.getDocRoot(), "class path") // class path

  @Singleton
  @Provides
  def providesThirdPartyFoo: FileResolver = {
    new FileResolver(keyFileDoc(), "")
  }
}
