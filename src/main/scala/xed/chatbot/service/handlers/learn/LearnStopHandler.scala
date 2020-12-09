package xed.chatbot.service.handlers.learn

import com.twitter.util.Future
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler

case class LearnStopHandler(nlpService: NLPService,
                            botConfig: BotConfig,
                            analytics: Analytics,
                            learnProcessor: LearnProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = Future {

    context.removeContextParam(BotContext.REVIEW_CTX)
    context.removeContextParam(BotContext.LEARN_TEST_CTX)
    context.removeContextParam(BotContext.LEARN_TEST_FOLLOWCTX)
    context.removeLearnContext()


    learnProcessor.sendHelpMessage(context)

  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  Future.Unit

}