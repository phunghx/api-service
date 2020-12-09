package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.domain.design.v100.Text
import xed.api_service.service.NLPService
import xed.chatbot.domain.{BotConfig, BotContext, ChatMessage, UserAction, XCard}


abstract class BaseItemSelector {

  protected lazy val clazz = getClass.getSimpleName

  protected val botConfig: BotConfig
  protected val nlpService: NLPService

  protected def isShowListingRequired(isFirstTime: Boolean) : Future[Boolean] =  Future.value(isFirstTime)

  final def setupAndShowListing(context: BotContext): Future[Boolean] = onShowListing(context)

  final def processUserReply(context: BotContext, chatMessage: ChatMessage): Future[Boolean] =  {

    val funcChain = Seq[( BotContext, ChatMessage) => Future[Boolean]] (
     handlePrevPage,
     handleNextPage,
     handleItemSelection,
     handleUnknownCommand
   )

    funcChain.foldLeft(Future.False)((result, fn) => result.flatMap{
      case true => Future.True
      case false => fn(context,chatMessage)
    })

  }


  private def handleItemSelection(context: BotContext, chatMessage: ChatMessage): Future[Boolean] = {

    def resolveItemIndices(message: ChatMessage): Option[Seq[Int]] = {
      message.text
        .flatMap(x => if (x == null || x.trim.isEmpty) None else Some(x.trim))
        .filter(answerText => answerText.matches("^(\\d+)([\\s,]*\\d+)*$"))
        .map(text => {
          "\\d+".r.findAllMatchIn(text)
            .map(_.group(0))
            .map(_.toInt - 1)
            .filter(_ >= 0)
            .toSeq
            .distinct
        })
    }

    val fn = resolveItemIndices( chatMessage) match {
      case Some(itemIndices) => onItemSelected(context,itemIndices).flatMap(_ => Future.True)
      case _ => Future.False
    }

    fn.rescue({
      case _ => onShowListing(context).map(_ => true)
    })

  }

  private def handlePrevPage(context: BotContext, chatMessage: ChatMessage): Future[Boolean] = {
    def isPrevPageCmd(message: ChatMessage): Boolean = {
      val action = botConfig.getPrevPageAction()
      action.isMatch(message.text.getOrElse(""))
    }

    isPrevPageCmd(chatMessage) match {
      case true => onPrevPage(context).map(_ => true)
      case _ => Future.False
    }
  }

  private def handleNextPage(context: BotContext, chatMessage: ChatMessage): Future[Boolean] = {
    def isNextPageCmd(message: ChatMessage): Boolean = {
      val action = botConfig.getNextPageAction()
      action.isMatch(message.text.getOrElse(""))
    }

    isNextPageCmd(chatMessage) match {
      case true => onNextPage(context).flatMap(_ => Future.True)
      case _ => Future.False
    }
  }

  private def handleUnknownCommand(context: BotContext, chatMessage: ChatMessage): Future[Boolean] = Future {
    buildUnknownCommandMessage(context).foreach(context.write)
    true
  }

  protected def buildUnknownCommandMessage(context: BotContext): Seq[ChatMessage] = {
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getUnrecognizedMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Some(context.recipient.username)
    )
    Seq(msg)
  }

  protected def onShowListing(context: BotContext): Future[Boolean]

  protected def onPrevPage(context: BotContext): Future[Unit]

  protected def onNextPage(context: BotContext): Future[Unit]

  protected def onItemSelected(context: BotContext, itemIndices: Seq[Int]): Future[Unit]

  protected def getActions(context: BotContext): Seq[UserAction]

  protected def getSuggestionActions(context: BotContext): Seq[UserAction]

}

