package xed.chatbot.service.handlers

import com.twitter.util.Future
import xed.api_service.domain.design.v100._
import xed.api_service.service._
import xed.chatbot.domain.Implicits._
import xed.chatbot.domain._
import xed.chatbot.service.DictionaryService
import xed.userprofile.UserProfileService

case class DictionaryProcessor(botConfig: BotConfig,
                               dictionaryService: DictionaryService,
                               profileService: UserProfileService) extends TryProcessor {
  val actionType = IntentActionType.DICT_SEARCH

  protected def setupFlowContext(botContext: BotContext, chatMessage: ChatMessage): Unit = {
    val sessionParam = DictSearchData(
      word = chatMessage.text
    )
    botContext.updateContextData(BotContext.SEARCH_DICT_CTX, sessionParam)
    botContext.updateContextData(BotContext.SEARCH_DICT_FOLLOWCTX, EmptyContextData())
  }

  protected def resetFlowContext(botContext: BotContext): Unit = {
    botContext.removeSearchDictParam()
  }

  protected def tryHandle(botContext: BotContext, chatMessage: ChatMessage): Future[Boolean] = {
    processSearch(botContext, true)
  }



  def processSearch(botContext: BotContext, isTryHandle: Boolean = false): Future[Boolean] = {

    val searchDictParam = botContext.getSearchDictParam()
    val word = searchDictParam.word.get
    for {
      targetLang <- getNativeLang(botContext)
      dictMap <- dictionaryService.lookup("en", targetLang, word).map({
        case Some(r) => r.toDictComponentMap(1)
        case _ => Map.empty[String, Dictionary]
      })

      r = if (dictMap.nonEmpty) {
        val partOfSpeech = dictMap.keys.head
        val otherPartOfSpeeches = dictMap.keys.filterNot(_ == partOfSpeech).toSeq

        val message = buildDictionaryMessage(
          botContext,
          dictMap.get(partOfSpeech).get,
          partOfSpeech,
          otherPartOfSpeeches)
        botContext.write(message)
        true
      } else {
        if (!isTryHandle)
          botContext.write(buildNoDictionaryMessage(botContext))
        false
      }
    } yield r match {
      case true =>
        botContext.removeLearnContext()
        true
      case _ => false
    }
  }

  def processPOSChanged(botContext: BotContext, userInputText: String): Future[Boolean] = {
    val searchDictParam = botContext.getSearchDictParam()
    val word = searchDictParam.word.get
    for {
      targetLang <- getNativeLang(botContext)
      dictMap <- dictionaryService.lookup("en", targetLang, word).map(r => {
        r.map(_.toDictComponentMap(1))
          .getOrElse(Map.empty)
      })

      partOfSpeeches = dictMap.keys.toSeq
      userPartOfSpeech = partOfSpeeches
        .filter(_.equalsIgnoreCase(userInputText))
        .headOption

    } yield userPartOfSpeech match {
      case Some(userPartOfSpeech) => sendDictionaryMessage(
        botContext,
        partOfSpeech = userPartOfSpeech,
        partOfSpeeches = partOfSpeeches,
        dictMap = dictMap)
        true
      case _ => false
    }
  }

  private def getNativeLang(botContext: BotContext): Future[String] = {
    for {
      profile <- profileService.getProfile(botContext.recipient.username)
      searchDictParam = botContext.getSearchDictParam()
      targetLang = searchDictParam.targetLang.flatMap(x => if (x == null || x.isEmpty) None else Some(x))
      r = if (targetLang.isDefined) targetLang
      else profile.flatMap(_.nativeLanguages).flatMap(_.headOption)

    } yield {
      r.getOrElse("en")
    }
  }

  def sendInvalidPOSMessage(botContext: BotContext): Unit = {
    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getDictionaryInvalidPosMessage())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq(
          PostBackUAction("Exit", "Exit")
        )
      )),
      currentAction = Option(botContext.actionInfo).map(_.copy(actionType = IntentActionType.DICT_SEARCH)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(botContext.recipient.username)
    )
    botContext.write(message)
  }


  private def sendDictionaryMessage(botContext: BotContext,
                                    partOfSpeech: String,
                                    partOfSpeeches: Seq[String],
                                    dictMap: Map[String, Dictionary]): Unit = {

    val component = dictMap.get(partOfSpeech).get
    val otherPos = partOfSpeeches.filterNot(_ == partOfSpeech)
    val message = buildDictionaryMessage(
      botContext,
      component,
      partOfSpeech,
      otherPos)
    botContext.write(message)
  }

  def sendNoDictionaryMessage(botContext: BotContext): Unit = {
    val message = buildNoDictionaryMessage(botContext)
    botContext.write(message)
  }

  protected def buildDictionaryMessage(botContext: BotContext,
                                       dictComponent: Dictionary,
                                       currentPartOfSpeech: String,
                                       otherPartOfSpeeches: Seq[String]): ChatMessage = {

    val suggestions = Seq(
      PostBackUAction("Exit", "Exit"),
      PostBackUAction(currentPartOfSpeech, currentPartOfSpeech)
    ) ++ otherPartOfSpeeches.map(pos => {
      PostBackUAction(pos, pos)
    })

    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          dictComponent
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = suggestions
      )),
      currentAction = Option(botContext.actionInfo).map(_.copy(actionType = IntentActionType.DICT_SEARCH)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(botContext.recipient.username)
    )

  }

  def buildNoDictionaryMessage(botContext: BotContext): ChatMessage = {

    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getDictionaryNotFoundMessage())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      currentAction = None,
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(botContext.recipient.username)
    )

  }

}

case class DictionarySearchHandler(nlpService: NLPService,
                                   botConfig: BotConfig,
                                   analytics: Analytics,
                                   processor: DictionaryProcessor) extends ActionHandler {

  override def handleCall(context: BotContext,
                          chatMessage: ChatMessage): Future[Unit] = {


    processor.processSearch(context,false).map(_ => {})

  }

  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {

    processor.processPOSChanged(
      context,
      chatMessage.text.getOrElse("")
    ).flatMap({
      case true => Future.True
      case _ => processor.tryHandleOnFallback(context, chatMessage)
    }).map({
      case true => {}
      case false => processor.sendNoDictionaryMessage(context)
    })
  }
}





case class DictionaryExitHandler(nlpService: NLPService,
                                 botConfig: BotConfig,
                                 analytics: Analytics,
                                 processor: DictionaryProcessor) extends ActionHandler {

  override def handleCall(context: BotContext, chatMessage: ChatMessage) = {

    context.removeSearchDictParam()
    processor.sendHelpMessage(context)
    Future.Unit

  }


  override def handleUserReply(context: BotContext, chatMessage: ChatMessage): Future[Unit] = {
    Future.Unit
  }

}



