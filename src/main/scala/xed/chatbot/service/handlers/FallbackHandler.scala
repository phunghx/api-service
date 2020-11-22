package xed.chatbot.service.handlers
import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService}
import xed.chatbot.domain._


case class FallbackHandler(nlpService: NLPService,
                           botConfig: BotConfig,
                           analytics: Analytics,
                           processors: Seq[TryProcessor]) extends ActionHandler {


  override def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit] =  {


    val fn = processors.foldLeft[Future[Boolean]](Future.False)((fn,processor) => fn flatMap {
      case true => Future.True
      case _ => processor.tryHandleOnFallback(
        botContext = context,
        chatMessage = chatMessage)
    })

    // If the message can't be handled => process it as an unrecognized message
    fn.flatMap({
      case true => Future.Unit
      case _ => handleUnrecognizedMessage(context,chatMessage)
    })

  }


  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

  private def handleUnrecognizedMessage(context: BotContext,
                                        chatMessage: ChatMessage ): Future[Unit] = Future {

    val message = buildUnrecognizedMessage(context, context.actionInfo, chatMessage)

    context.write(message)
  }

  private def buildUnrecognizedMessage(botContext: BotContext,
                                       actionInfo: IntentActionInfo,
                                       chatMessage: ChatMessage): ChatMessage = {
    val card = XCard(
      version = 1,
      body = Seq(
        Text(botConfig.getUnrecognizedMsg())
      ),
      actions = Seq.empty,
      fallbackText = None,
      background = None,
      speak = None,
      suggestions = Seq.empty
    )


    ChatMessage.create(
      card = Some(card),
      currentAction = if (actionInfo.isRequiredParamCompleted && actionInfo.contexts.nonEmpty) Some(actionInfo) else None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Some(botContext.recipient.username)
    )
  }
}

