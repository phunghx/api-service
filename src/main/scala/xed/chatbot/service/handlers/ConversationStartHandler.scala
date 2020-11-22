package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.domain.SRSSource
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService, SRSService}
import xed.chatbot.domain._
import xed.userprofile.UserProfileService

case class ConversationStartHandler(nlpService: NLPService,
                                    botConfig: BotConfig,
                                    analytics: Analytics,
                                    srsService: SRSService,
                                    profileService: UserProfileService) extends ActionHandler {


  override def handleCall(context: BotContext,
                          chatMessage: ChatMessage): Future[Unit] =  {
    isOnBoarding(context).flatMap({
      case true => setupOnBoarding(context)
      case _ => handleConversationStart(context)
    })
  }


  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

  private def isOnBoarding(context: BotContext) = {
    //Always return false for now. Do later
    profileService.getProfile(context.recipient.username)
      .map(_.get)
      .map(_ => false)
  }

  private def setupOnBoarding(context: BotContext): Future[Unit] = {
    Future.Unit
  }

  private def handleConversationStart(context: BotContext): Future[Unit] = {
    for {
      dueCardCount <- srsService.getTotalDueCardBySource(context.recipient, SRSSource.BOT)
      msg = if(dueCardCount > 0)
        buildSuggestReviewMessage(context, dueCardCount)
      else
        buildSuggestLearnVocabulary(context)
    } yield {
      context.write(msg)
    }
  }

  private def buildSuggestReviewMessage(context: BotContext, dueCardCount: Long)  = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getHasDueCardSuggestedMsg(dueCardCount))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq(
          PostBackUAction(botConfig.getReviewAction().title, botConfig.getReviewAction().value)
        )
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

  }

  private def buildSuggestLearnVocabulary(context: BotContext)= {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getSuggestLearnMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq(
          PostBackUAction(botConfig.getLearnAction().title, botConfig.getLearnAction().value)
        )
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }
}
