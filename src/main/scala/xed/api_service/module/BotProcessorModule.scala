package xed.api_service.module

import com.google.inject.{Inject, Provides, Singleton}
import com.twitter.inject.TwitterModule
import xed.api_service.service.{MessageService => _}
import xed.chatbot.domain.BotConfig
import xed.chatbot.service._
import xed.chatbot.service.handlers._
import xed.userprofile.UserProfileService

object BotProcessorModule extends BotProcessorModule {

}

class BotProcessorModule extends TwitterModule {

  override def configure(): Unit = {
    super.configure()

  }

  @Provides
  @Singleton
  def providesBotProcessors(@Inject botConfig: BotConfig,
                            dictionaryService: DictionaryService,
                            profileService: UserProfileService): Seq[TryProcessor] = {
    val dictionaryProcessor = DictionaryProcessor(
      botConfig,
      dictionaryService,
      profileService
    )
    Seq(
      dictionaryProcessor
    )
  }


}

