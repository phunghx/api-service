package xed.chatbot.service.handlers.challenge

import com.twitter.util.Future
import xed.api_service.domain.design.v100.{Answer, MultiChoice, Text}
import xed.api_service.domain.exception.NotFoundError
import xed.api_service.service.NLPService
import xed.chatbot.domain._
import xed.chatbot.domain.challenge.{Challenge, ChallengeTemplate}
import xed.chatbot.service.handlers.BaseItemSelector
import xed.chatbot.service.{ChallengeService, ChallengeTemplateService}

import scala.collection.mutable.ListBuffer


case class MyChallengeSelector(nlpService: NLPService,
                               botConfig: BotConfig,
                               challengeService: ChallengeService) extends BaseItemSelector {

  override protected def onShowListing(context: BotContext): Future[Boolean] = {

    val pagingData = context.getChallengePagingParam()
    for {
      openingChallenges <- challengeService.getMyOpeningChallenges(context.recipient.username)
      challengeIds = openingChallenges.map(_.challengeId.toString)
      totalChallengeCount = openingChallenges.size

    } yield {
      context.updateChallengePagingParam(pagingData.copy(
        totalItemCount = totalChallengeCount,
        from = if (openingChallenges.isEmpty) Some(0.0) else pagingData.from,
        size = Some(challengeIds.size),
        ids = Option(challengeIds)
      ))

      if (challengeIds.nonEmpty) {
        context.write(buildListingMsg(context, openingChallenges))
        true
      } else false
    }
  }

  override protected def onPrevPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getChallengePagingParam()
    for {
      ok <- Future.value(prevPage(context))
      templates <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) - botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        challengeService.getMyOpeningChallenges(context.recipient.username)
      } else Future.value(Seq.empty)
      templateIds = templates.map(_.challengeId.toString)
    } yield {
      pagingData = pagingData.copy(ids = Some(templateIds))
      context.updateChallengePagingParam(pagingData)
      context.write(buildListingMsg(context, templates))
    }
  }

  override protected def onNextPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getChallengePagingParam()
    for {
      ok <- Future.value(nextPage(context))
      templates <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) + botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        challengeService.getMyOpeningChallenges(context.recipient.username)
      } else Future.value(Seq.empty)
      templateIds = templates.map(_.challengeId.toString)
    } yield {
      pagingData = pagingData.copy(ids = Some(templateIds))
      context.updateChallengePagingParam(pagingData)
      context.write(buildListingMsg(context, templates))
    }
  }

  private def buildListingMsg(context: BotContext, items: Seq[Challenge]): ChatMessage = {
    val question = MultiChoice(
      question = botConfig.getChallengeListingMsg(),
      answers = items.foldLeft(ListBuffer.empty[Answer])((answers, item) => {
        answers.append(Answer(
          text = item.name
        ))
        answers
      })
    )

    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(question),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = getActions(context)
      )),
      currentAction = Option(context.actionInfo).map(_.copy(
        actionType = IntentActionType.CHALLENGE_JOIN
      )),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  private def prevPage(context: BotContext): Boolean = {
    val param = context.getChallengePagingParam()
    if (param.canPaging(false)) {
      context.updateChallengePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    } else false

  }

  private def nextPage(context: BotContext): Boolean = {
    val param = context.getChallengePagingParam()
    if (param.canPaging(true)) {
      context.updateChallengePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    } else false

  }

  override protected def onItemSelected(context: BotContext, indices: Seq[Int]): Future[Unit] = {
    val param = context.getChallengePagingParam()
    val itemId = param.ids.getOrElse(Seq.empty) lift indices.head
    for {
      challenge <- itemId match {
        case Some(id) => challengeService.getChallengeInfo(id.toInt)
        case _ => Future.exception(NotFoundError(Some("This challenge is no longer exist.")))
      }
      questionCount <- challengeService.getQuestionCount(challenge.questionListId)
    } yield {
      context.removeContextParam(BotContext.CHALLENGE_PAGING_CTX)
      context.removeContextParam(BotContext.CHALLENGE_JOIN_CTX)
      val msg = buildChallengeMessage(context, challenge, questionCount)
      context.write(msg)
    }
  }

  private def buildChallengeMessage(context: BotContext,
                                    challenge: Challenge,
                                    questionCount: Int): ChatMessage = {

    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getChallengeIntroMessage(challenge, questionCount))
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions =  Seq(
          PostBackUAction("Play", s"Play challenge ${challenge.challengeId}")
        )
      )),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    message
  }

  override def getActions(context: BotContext): Seq[UserAction] = {
    val param = context.getChallengePagingParam()

    val actions = ListBuffer.empty[UserAction]
    if (param.canPaging(false))
      actions.append(PostBackUAction(
        botConfig.getPrevPageAction().title,
        botConfig.getPrevPageAction().value))

    if (param.canPaging(true))
      actions.append(PostBackUAction(
        botConfig.getNextPageAction().title,
        botConfig.getNextPageAction().value))
    actions
  }

  override def getSuggestionActions(context: BotContext): Seq[UserAction] = {
    val actions = ListBuffer.empty[UserAction]
    actions
  }

  override protected def buildUnknownCommandMessage(context: BotContext): Seq[ChatMessage] = {
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
}


