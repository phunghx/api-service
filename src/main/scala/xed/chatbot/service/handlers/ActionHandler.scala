package xed.chatbot.service.handlers

import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.{Analytics, NLPService}
import xed.api_service.util.{JsonUtils, ZConfig}
import xed.chatbot.domain._
import xed.profiler.Profiler
import xed.userprofile.SignedInUser

abstract class ActionHandler extends Logging {

  protected lazy val clazz = getClass.getSimpleName

  private val testerIds = ZConfig.getStringList("srs.testers",List.empty).toSet
  protected val botConfig: BotConfig
  protected val nlpService: NLPService
  protected val analytics: Analytics

  final def handle(user: SignedInUser,
                   actionInfo: IntentActionInfo,
                   isFirstTime: Boolean,
                   chatMessage: ChatMessage): Future[Seq[ChatMessage]] =  {


    val botContext = BotContext(nlpService,user, actionInfo)
    sendDebug(botContext, isFirstTime, chatMessage)
    for {

      _ <- if(isFirstTime) Profiler(s"$clazz.handleCall") {
          handleCall(botContext, chatMessage)
        }
      else  Profiler(s"$clazz.handleUserReply") {
        handleUserReply(botContext, chatMessage)
      }
    } yield {
      botContext.getMessages
    }

  }


  /**
   * Xảy ra khi lần đầu (action + intent) này được gọi.
   * @param context
   * @param chatMessage
   * @return
   */
  def handleCall(context: BotContext, chatMessage: ChatMessage): Future[Unit]

  /**
   * Xảy ra khi user gửi 1 reply kể từ sau khi ${handleCall} kết thúc
   * @param context
   * @param chatMessage
   * @return
   */
  def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit]



  def handleExit(context: BotContext, chatMessage: ChatMessage): Future[Unit] = Future {
    val msg = ActionHandler.buildHelpMessage(context, botConfig)
    if(msg!=null)
      context.write(msg)
  }



  private def sendDebug(context: BotContext,
                        isFirstTime: Boolean,
                        chatMessage: ChatMessage): Unit = {

    val actionInfo =  context.actionInfo

    if(ZConfig.getBoolean("bot.debug", false) || testerIds.contains(context.recipient.username)) {
      val message = ChatMessage.create(
        card = Some(XCard(
          version = 1,
          body = Seq(
            Text(
              s"""
                 |DEBUG
                 |
                 |Message: ${Option(chatMessage).flatMap(_.text).getOrElse("")}
                 |Consider as: ${actionInfo.actionType} with confidence = ${actionInfo.confidence.getOrElse(0.0f)}
                 |Handled by $clazz""".stripMargin)
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
      context.write(message)
    }
  }



  def parseSuggestionsFromPayload(actionInfo: IntentActionInfo): Seq[UserAction] = {
     actionInfo.parseSuggestionsFromPayload()
  }

  def parseActionsFromPayload(actionInfo: IntentActionInfo): Seq[UserAction] = {
     actionInfo.parseActionsFromPayload()
  }
}


object  ActionHandler {

  def buildHelpMessage(context: BotContext, botConfig: BotConfig) : ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getHelpMsg())
        ),
        actions = Seq(
          PostBackUAction(botConfig.getLearnAction().title, botConfig.getLearnAction().value),
          PostBackUAction(botConfig.getReviewAction().title, botConfig.getReviewAction().value)
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


