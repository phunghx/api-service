package xed.chatbot.service.handlers.challenge

import com.twitter.util.Future
import xed.api_service.domain.design.v100.{FillInBlank, MultiChoice, MultiSelect}
import xed.api_service.service.{Analytics, NLPService, Operation}
import xed.api_service.util.Implicits.ImplicitMultiSelectToMultiChoice
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.{BaseFIBHandler, BaseMCHandler, TestProcessor}

case class ChallengePlayHandler(nlpService: NLPService,
                                botConfig: BotConfig,
                                analytics: Analytics,
                                processor: TestProcessor,
                                fibHandler: BaseFIBHandler,
                                mcHandler: BaseMCHandler) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
     processor.startWith(context).map(_ => {
       val challengeData = context.getChallengeContextData()
       analytics.log(Operation.BOT_PLAY_CHALLENGE, context.recipient.userProfile, Map(
         "username" -> context.recipient.username,
         "challenge_id" -> challengeData.challengeId,
         "challenge" -> challengeData
       ))
     })
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    for {
      (cardId, component) <- processor.getCurrentComponentToReview(context)
      isCompleted <- component match {
        case Some(component: FillInBlank) => fibHandler.handleUserAnswer(context, chatMessage, cardId, component)
        case Some(component: MultiChoice) => mcHandler.handleUserAnswer(context, chatMessage, cardId, component)
        case Some(component: MultiSelect) => mcHandler.handleUserAnswer(context, chatMessage, cardId, component.asMultiChoice())
        case _ => Future.False
      }

      _ <- if (!isCompleted) Future.Unit else {
        processor.onQuestionCompleted(context, cardId)
      }
    } yield {
      val challengeData = context.getChallengeContextData()
      context.updateChallengeContext(challengeData)
    }
  }

  override def handleExit(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    val challengeData = context.getChallengeContextData()
    challengeData.challengeId match {
      case Some(_) =>  processor.submitScoreAndCompleteChallenge(context)
      case _ => super.handleExit(context, chatMessage)
    }
  }
}

