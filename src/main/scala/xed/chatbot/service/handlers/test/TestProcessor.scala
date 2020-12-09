package xed.chatbot.service.handlers.test

import com.twitter.util.Future
import xed.api_service.domain.design.Component
import xed.api_service.domain.design.v100.Text
import xed.api_service.domain.{Card, SRSCard, SRSSource}
import xed.api_service.service.{CardService, SRSService}
import xed.chatbot.domain._
import xed.chatbot.service.handlers.Processor
import xed.profiler.Profiler

import scala.collection.mutable.ListBuffer
/**
 * @author andy
 * @since 2/17/20
 **/


abstract class TestProcessor extends Processor {


  val srsService: SRSService
  val cardService: CardService


  def startWith(context: BotContext): Future[Unit]

  def setupContextAndStart(context: BotContext, cardIds: Seq[String]): Future[Unit]

  def submitAnswerAndUpdateScore(context: BotContext, cardId: String, isCorrect: Boolean, isSkip: Boolean = false): Future[Boolean]

  def onQuestionCompleted(context: BotContext, cardId: String): Future[Unit]

  def submitScoreAndCompleteChallenge(context: BotContext) : Future[Unit]

  def sendNoCardExplainMessage(context: BotContext): Unit

  def sendTotalCardExplainMessage(context: BotContext, totalCard: Int): Unit


  final def initAndStart(context: BotContext, cardIds: Seq[String]): Future[Unit] = Profiler(s"$clazz.initAndStart") {
    for {
      _ <- Future.value(context.removeLearnContext())
      _ = if (cardIds.nonEmpty)
        sendTotalCardExplainMessage(context, cardIds.size)
      else
        sendNoCardExplainMessage(context)

      _ <- if (cardIds.nonEmpty) {
        val data =  ExaminationData(
          cardIds,
          totalCount = cardIds.size,
          -1,
          0,
          ListBuffer.empty[String])
        context.updateContextData(BotContext.EXAMINATION_DATA,data)
        gotoNextQuestion(context).flatMap(_ => sendQuestionMessage(context))
      } else {
        Future.Unit
      }
    } yield {

    }

  }

  protected def gotoNextQuestion(context: BotContext): Future[Boolean]

  final def getCurrentCardIdToReview(context: BotContext) = {

    val reviewParam = context.getExaminationData()

    val cardId = reviewParam.getCardId().get

    Future.value(cardId)
  }

  final def getCurrentComponentToReview(context: BotContext): Future[(String, Option[Component])] = {

    val reviewParam = context.getExaminationData()
    val frontIndex = reviewParam.currentFrontIndex
    val cardId = reviewParam.getCardId().get

    cardService.getCard(cardId)
      .map(card => SRSCard(cardId, card))
      .map(_.getFrontAt(Some(frontIndex)))
      .map({
        case (frontCard, _) => (
          cardId,
          frontCard.flatMap(_.getActionComponent())
        )
      })
  }


  def buildQuestionMessage(context: BotContext): Future[ChatMessage]

  final def sendQuestionMessage(context: BotContext): Future[Unit] = Profiler(s"${clazz}.sendQuestionMessage") {
    buildQuestionMessage(context).flatMap(message =>  {
      context.write(message)
      Future.Unit
    })
  }

  final def sendDictionaryBackCard(context: BotContext,
                                   cardId: String,
                                   enableLearnSuggestion: Boolean = false): Future[Boolean] = {

    for {
      card <- cardService.getCard(cardId)
      r <- sendDictionaryBackCard(context, card, enableLearnSuggestion)
    } yield {
      r
    }

  }

  def sendDictionaryBackCard(context: BotContext,
                                   card: Card,
                                   enableLearnSuggestion: Boolean): Future[Boolean] = Future {

    val backCard = card.backCard()

    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          backCard
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = if (enableLearnSuggestion) Seq(
          PostBackUAction(botConfig.getLearnYesAction().title, botConfig.getLearnYesAction().value),
          PostBackUAction(botConfig.getLearnNoAction().title, botConfig.getLearnNoAction().value),
          PostBackUAction("Exit", "Exit")
        )
        else Seq.empty
      )),
      currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

    true
  }


  final def sendNextDueDateMessage(context: BotContext, cardId: String) = {
    val examinationData = context.getExaminationData()
    for {
      reviewCard <- srsService.getReviewCards(context.recipient, SRSSource.BOT, Seq(cardId)).map(_.headOption)
    } yield {
      reviewCard.foreach(reviewCard => {
        val msg = ChatMessage.create(
          card = Some(XCard(
            version = 1,
            body = Seq(
              Text(botConfig.getLearnWordCompletedAndDudeDateMsg(
                name = reviewCard.card.name.getOrElse(""),
                dudeDateStr = reviewCard.getDueDateAsReadableStr()
              ))
            ),
            actions = Seq.empty,
            fallbackText = None,
            background = None,
            speak = None,
            suggestions = Seq(
              PostBackUAction(s"Next Question (${examinationData.getTotalAnsweredQuestions()}/${examinationData.totalCount})", "Next Question")
            )
          )),
          currentAction = Option(context.actionInfo).map(_.copy(actionType = actionType)),
          sender = Some(ChatMessage.KIKIBOT),
          recipient = Option(context.recipient.username)
        )

        context.write(msg)
      })
    }
  }

  def buildResultMessage(context: BotContext, examinationData: ExaminationData): ChatMessage = {
    val correctCount = examinationData.getCorrectQuestionCount()
    val totalCount = examinationData.getTotalAnsweredQuestions()
    val passedPercent = (if (totalCount > 0) correctCount / totalCount.toDouble else 0.0) * 100

    val message = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getReviewResultMsg(passedPercent, correctCount, totalCount))
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

    message
  }


}
