package xed.chatbot.service.handlers.learn
import com.twitter.util.Future
import xed.api_service.domain.design.v100.{FillInBlank, MultiChoice, MultiSelect}
import xed.api_service.service.{Analytics, NLPService}
import xed.api_service.util.Implicits.ImplicitMultiSelectToMultiChoice
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.{BaseFIBHandler, BaseMCHandler, TestProcessor}

case class LearnTestHandler(nlpService: NLPService,
                            botConfig: BotConfig,
                            analytics: Analytics,
                            testProcessor: TestProcessor,
                            fibHandler: BaseFIBHandler,
                            mcHandler: BaseMCHandler) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    //Remove learn context in order to move to test flow.
    context.removeLearnContext()
    testProcessor.startWith(context).map(_ => {
      context.updateContextData(BotContext.LEARN_TEST_CTX,EmptyContextData())
      context.updateContextData(BotContext.LEARN_TEST_FOLLOWCTX,EmptyContextData())
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
      _ = {
        context.updateContextData(BotContext.LEARN_TEST_CTX,EmptyContextData())
        context.updateContextData(BotContext.LEARN_TEST_FOLLOWCTX,EmptyContextData())
      }
      _ <- if (!isCompleted) Future.Unit else {
        testProcessor.onQuestionCompleted(context, cardId)
      }
    } yield {

    }
  }


}