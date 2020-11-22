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



case class ChallengeTemplateSelector(nlpService: NLPService,
                                     botConfig: BotConfig,
                                     challengeTemplateService: ChallengeTemplateService,
                                     challengeService: ChallengeService
                                    ) extends BaseItemSelector {

  override protected def onShowListing(context: BotContext): Future[Boolean] = {

    val pagingData = context.getChallengeTemplatePagingParam()
    for {
      r <- challengeTemplateService.searchChallengeTemplates(
        from = pagingData.from.getOrElse(0.0).toInt,
        size = botConfig.listingItemSize
      )
      templates = r.records
      templateIds = r.records.map(_.templateId)
      totalTemplateCount = r.total.toInt

    } yield {
      context.updateChallengeTemplatePagingParam(pagingData.copy(
        totalItemCount = totalTemplateCount,
        from = if (templates.isEmpty) Some(0.0) else pagingData.from,
        size = Some(botConfig.listingItemSize),
        ids = Option(templateIds)
      ))

      if (templateIds.nonEmpty) {
        context.write(buildListingMsg(context, templates))
        true
      } else false
    }
  }

  override protected def onPrevPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getChallengeTemplatePagingParam()
    for {
      ok <- Future.value(prevPage(context))
      templates <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) - botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        challengeTemplateService.searchChallengeTemplates(
          from = pagingData.from.getOrElse(0.0).toInt,
          size = botConfig.listingItemSize).map(_.records)
      } else Future.value(Seq.empty)
      templateIds = templates.map(_.templateId)
    } yield {
      pagingData = pagingData.copy(ids = Some(templateIds))
      context.updateChallengeTemplatePagingParam(pagingData)
      context.write(buildListingMsg(context, templates))
    }
  }

  override protected def onNextPage(context: BotContext): Future[Unit] = {
    var pagingData = context.getChallengeTemplatePagingParam()
    for {
      ok <- Future.value(nextPage(context))
      templates <- if (ok) {
        pagingData = pagingData.copy(
          totalItemCount = pagingData.totalItemCount,
          from = Some(pagingData.from.getOrElse(0.0) + botConfig.listingItemSize),
          size = Some(botConfig.listingItemSize)
        )
        challengeTemplateService.searchChallengeTemplates(
          from = pagingData.from.getOrElse(0.0).toInt,
          size = botConfig.listingItemSize).map(_.records)
      } else Future.value(Seq.empty)
      templateIds = templates.map(_.templateId)
    } yield {
      pagingData = pagingData.copy(ids = Some(templateIds))
      context.updateChallengeTemplatePagingParam(pagingData)
      context.write(buildListingMsg(context, templates))
    }
  }

  private def buildListingMsg(context: BotContext, items: Seq[ChallengeTemplate]): ChatMessage = {
    val question = MultiChoice(
      question = botConfig.getChallengeListingMsg(),
      answers = items.foldLeft(ListBuffer.empty[Answer])((answers, item) => {
        answers.append(Answer(
          text = Some(item.name)
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
        actionType = IntentActionType.CHALLENGE_CREATE
      )),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  private def prevPage(context: BotContext): Boolean = {
    val param = context.getChallengeTemplatePagingParam()
    if (param.canPaging(false)) {
      context.updateChallengeTemplatePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    } else false

  }

  private def nextPage(context: BotContext): Boolean = {
    val param = context.getChallengeTemplatePagingParam()
    if (param.canPaging(true)) {
      context.updateChallengeTemplatePagingParam(param.copy(
        totalItemCount = param.totalItemCount,
        from = Some(param.from.getOrElse(0.0) + botConfig.listingItemSize),
        size = Some(botConfig.listingItemSize)
      ))
      true
    } else false

  }

  override protected def onItemSelected(context: BotContext, indices: Seq[Int]): Future[Unit] = {
    val param = context.getChallengeTemplatePagingParam()
    val itemId = param.ids.getOrElse(Seq.empty) lift indices.head
    for {
      template <- itemId match {
        case Some(templateId) => challengeTemplateService.getChallengeTemplate(templateId)
        case _ => Future.exception(NotFoundError(Some("This challenge is no longer exist.")))
      }
      questionCount <- challengeTemplateService.getQuestionCount(template.questionListId)
      challenge <- challengeService.createChallengeFromTemplate(
        context.recipient.username,
        template.templateId
      )
    } yield {
      context.removeContextParam(BotContext.CHALLENGE_TEMPLATE_PAGING_CTX)
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
    val param = context.getChallengeTemplatePagingParam()

    val actions = ListBuffer.empty[UserAction]
    if (param.canPaging(false))
      actions.append(PostBackUAction(botConfig.getPrevPageAction().title, botConfig.getPrevPageAction().value))

    if (param.canPaging(true))
      actions.append(PostBackUAction(botConfig.getNextPageAction().title, botConfig.getNextPageAction().value))
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


