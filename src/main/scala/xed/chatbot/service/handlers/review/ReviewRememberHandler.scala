package xed.chatbot.service.handlers.review

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.ActionHandler
import xed.chatbot.service.handlers.test.TestProcessor

/**
 * @author andy
 * @since 2/17/20
 **/
case class ReviewRememberHandler(nlpService: NLPService,
                                 botConfig: BotConfig,
                                 analytics: Analytics,
                                 processor: TestProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {

    for {
      (cardId, component) <- processor.getCurrentComponentToReview(context)
      isCompleted <- component match {
        case Some(_) => processor.sendQuestionMessage(context).map(_ => false)
        case _ =>
          processor.submitAnswerAndUpdateScore(context,cardId,true).map(x =>{
            sendReplied(context)
            x
          })
      }
      _ <- if (!isCompleted) Future.Unit else {
        processor.onQuestionCompleted(context, cardId)
      }
    } yield {
      context.updateContextData(BotContext.REVIEW_CTX,EmptyContextData())
    }
  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }


  private def sendReplied(context: BotContext): Unit = {
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getReviewCorrectMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = IntentActionType.REVIEW)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }
}
