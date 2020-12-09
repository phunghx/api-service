package xed.chatbot.service.handlers

import com.twitter.inject.Logging
import com.twitter.util.{Future, Return, Throw}
import xed.api_service.domain.design.v100.Text
import xed.chatbot.domain.{BotConfig, BotContext, ChatMessage, PostBackUAction, XCard}
import xed.profiler.Profiler

/**
 * @author andy
 * @since 2/8/20
 **/


abstract class Processor extends  Logging {

  lazy val clazz = getClass.getSimpleName
  val actionType: String
  val botConfig: BotConfig



  final def sendExitMessage(context: BotContext) = {
    val msg = buildExitMessage(context)
      if(msg!=null)
        context.write(msg)
  }


  final def sendHelpMessage(context: BotContext) = {
    val msg = ActionHandler.buildHelpMessage(context, botConfig)
    if(msg!=null)
      context.write(msg)
  }

  final def sendSuggestToLearnMessage(context: BotContext) = {
    val msg = buildSuggestToLearnMessage(context)
    if(msg!=null)
      context.write(msg)
  }

  def buildExitMessage(context: BotContext) : ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(
            """Great! Keep up your work.""".stripMargin)
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  def buildSuggestToLearnMessage(context: BotContext): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getAfterReviewSuggestedMsg())
        ),
        actions = Seq(
          PostBackUAction(
            botConfig.getLearnAction().title,
            botConfig.getLearnAction().value
          )
        ),
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }
}


abstract class TryProcessor extends Processor {

  /**
   * Xảy ra khi nhận đc 1 fallback intent (từ dialogflow)
   * Thử handle message xem có handle được hay không.
   * Nếu không thì sẽ trả về false để cho processor khác xử lý.
   * @param chatMessage
   * @return true nếu handle được và ngược lại
   */
  def tryHandleOnFallback(botContext: BotContext, chatMessage: ChatMessage): Future[Boolean] = Profiler(s"$clazz.tryHandleOnFallback") {
    setupFlowContext(botContext,chatMessage)
    tryHandle(botContext,chatMessage).transform({
      case Return(r) => Future.value(r)
      case Throw(e) => Future.False
    }).map({
      case false =>
        resetFlowContext(botContext)
        false
      case r => r
    })
  }

  protected  def setupFlowContext(botContext: BotContext, chatMessage: ChatMessage): Unit

  protected def resetFlowContext(botContext: BotContext): Unit

  /**
   * Thử handle message xem có handle được hay không.
   * Nếu không thì sẽ trả về false để cho processor khác xử lý.
   * @param chatMessage
   * @return true nếu handle được và ngược lại
   */
  protected def tryHandle(botContext: BotContext, chatMessage: ChatMessage): Future[Boolean]



}



case class DefaultProcessor(actionType: String,
                            botConfig: BotConfig) extends Processor