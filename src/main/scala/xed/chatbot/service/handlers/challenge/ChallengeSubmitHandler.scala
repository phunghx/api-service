package xed.chatbot.service.handlers.challenge

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService, Operation}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor


case class ChallengeSubmitHandler(nlpService: NLPService,
                                  botConfig: BotConfig,
                                  analytics: Analytics,
                                  processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    val challengeData = context.getChallengeContextData()
    analytics.log(Operation.BOT_SUBMIT_CHALLENGE, context.recipient.userProfile, Map(
      "username" -> context.recipient.username,
      "challenge_id" -> challengeData.challengeId
    ))
    processor.submitScoreAndCompleteChallenge(context)
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }


}
