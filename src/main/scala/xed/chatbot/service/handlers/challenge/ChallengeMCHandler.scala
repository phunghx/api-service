package xed.chatbot.service.handlers.challenge

import com.twitter.util.Future
import xed.api_service.domain.design.Component
import xed.api_service.domain.design.v100.{MultiChoice, Text}
import xed.api_service.util.ZConfig
import xed.chatbot.domain._
import xed.chatbot.service.handlers.test.{BaseMCHandler, TestProcessor}

import scala.collection.mutable.ListBuffer

/**
 * @author andy
 * @since 2/17/20
 **/
case class ChallengeMCHandler(botConfig: BotConfig, processor: TestProcessor) extends BaseMCHandler {

  override def handleUserAnswer(context: BotContext, chatMessage: ChatMessage, cardId: String, component: MultiChoice): Future[Boolean] = {
    super.handleUserAnswer(context, chatMessage, cardId, component).foreach(isCompleted => {
      if(isCompleted) {
        sendCompletionMessage(context)
      }
    })
  }


  /**
   * Dont send the correct answer in challenge mode.
   * @param context
   * @param component
   */
  override def sendIncorrectMsg(context: BotContext, component: MultiChoice): Unit = {
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getReviewIncorrectMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        effectId = Some(ZConfig.getString("bot.effect_answer_incorrect")),
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(1300),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }


  private def sendCompletionMessage(context: BotContext): Unit = {
    val examinationData = context.getExaminationData()
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getChallengeCorrectMsg(examinationData))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        effectId = None,
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(1000),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }

}
