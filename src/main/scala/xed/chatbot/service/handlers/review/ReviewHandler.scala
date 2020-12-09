package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.domain.design.v100.{FillInBlank, MultiChoice, MultiSelect}
import xed.api_service.service.{Analytics, NLPService, Operation}
import xed.api_service.util.Implicits.ImplicitMultiSelectToMultiChoice
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.{BaseFIBHandler, BaseMCHandler, TestProcessor}

case class ReviewHandler(nlpService: NLPService,
                         botConfig: BotConfig,
                         analytics: Analytics,
                         testProcessor: TestProcessor,
                         fibHandler: BaseFIBHandler,
                         mcHandler: BaseMCHandler) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage) : Future[Unit] = {
    context.removeLearnContext()
    testProcessor.startWith(context).map(_ => {
      analytics.log(Operation.BOT_START_REVIEW,
        context.recipient.userProfile,
        Map(
          "username" -> context.recipient.username
        )
      )
      context.updateContextData(BotContext.REVIEW_CTX,EmptyContextData())
    })
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {

    for {
      (cardId, component) <- testProcessor.getCurrentComponentToReview(context)
      isCompleted <- component match {
        case Some(fib: FillInBlank) => fibHandler.handleUserAnswer(context, chatMessage, cardId, fib)
        case Some(choice: MultiChoice) => mcHandler.handleUserAnswer(context, chatMessage, cardId, choice)
        case Some(select: MultiSelect) => mcHandler.handleUserAnswer(context, chatMessage, cardId, select.asMultiChoice())
        case _ => Future.False
      }

      _ <- if (!isCompleted) Future.Unit else {
        testProcessor.onQuestionCompleted(context,cardId)
      }
    } yield {
      context.updateContextData(BotContext.REVIEW_CTX,EmptyContextData())
    }
  }





}

