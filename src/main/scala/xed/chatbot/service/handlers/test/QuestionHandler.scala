package xed.chatbot.service.handlers.test

import com.twitter.util.Future
import xed.api_service.domain.Card
import xed.api_service.domain.design.Component
import xed.api_service.domain.design.v100.{FillInBlank, MultiChoice, Text}
import xed.api_service.util.{Utils, ZConfig}
import xed.chatbot.domain._
import xed.profiler.Profiler

import scala.collection.mutable.ListBuffer
import xed.api_service.domain.design.v100.{FillInBlank, Text}
import xed.api_service.util.Implicits.ImplicitString
import xed.chatbot.domain._

abstract class QuestionHandler[A, T <: Component] {
  lazy val clazz = getClass.getSimpleName

  protected val botConfig: BotConfig
  protected val processor: TestProcessor

  /**
   * Return true when you are done with this question
   * @param context
   * @param chatMessage
   * @param cardId
   * @param component
   * @return
   */
  def handleUserAnswer(context: BotContext,
                             chatMessage: ChatMessage,
                             cardId: String,
                             component: T): Future[Boolean] = Profiler(s"$clazz.handleUserAnswer") {

    val examinationData = context.getExaminationData()

    resolveUserAnswer(chatMessage).fold(handleInvalidInput(context))(answer => {
      checkPartialAnswerAndShowResult(context, examinationData, component, answer)
      if (isAnswerCompleted(examinationData, component)) {
        submitCompletedAnswerAndUpdateScore(
          context = context,
          examinationData = examinationData,
          cardId = cardId,
          component = component)
      } else {
        Future.False
      }
    })
  }



  private def handleInvalidInput(context: BotContext): Future[Boolean] = {
    val msg = buildInvalidInputAnswerMessage(context)
    context.write(msg)

    processor.sendQuestionMessage(context).map(_ => false)
  }

  private def submitCompletedAnswerAndUpdateScore(context: BotContext,
                                                  examinationData: ExaminationData,
                                                  cardId: String,
                                                  component: T): Future[Boolean] = {

    val isCorrect = validateResult(examinationData, component)
    processor.submitAnswerAndUpdateScore(context,cardId,isCorrect)
  }

  protected def sendVocabularyCard(context: BotContext, card: Card) : Future[Boolean] = Future {

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
        suggestions = Seq.empty
      )),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
    context.write(msg)

    true
  }

  def resolveUserAnswer(chatMessage: ChatMessage) : Option[A]


  def isAnswerCompleted(reviewParam: ExaminationData, component: T): Boolean


  def buildInvalidInputAnswerMessage(context: BotContext) : ChatMessage


  def checkPartialAnswerAndShowResult(context: BotContext,
                                      reviewParam: ExaminationData,
                                      component: T,
                                      answer: A)

  def validateResult(reviewParam: ExaminationData, component: T) : Boolean
}

abstract class BaseFIBHandler extends QuestionHandler[String,FillInBlank] {

  override def isAnswerCompleted(reviewParam: ExaminationData, component: FillInBlank): Boolean = {
    reviewParam.getCurrentFIBAnswerCount() >= component.getCorrectAnswerCount()
  }



  override def resolveUserAnswer(chatMessage: ChatMessage): Option[String] = {
    chatMessage.text.flatMap(_.asOption)
  }


  override def checkPartialAnswerAndShowResult(context: BotContext,
                                               examinationData: ExaminationData,
                                               component: FillInBlank,
                                               answer: String) = {

    val pos = examinationData.getCurrentFIBAnswerCount()

    (component.correctAnswers lift pos)
      .flatMap(_.asOption)
      .foreach(correctAnswer => {
        examinationData.addFIBAnswer(answer)
        context.updateContextData(BotContext.EXAMINATION_DATA, examinationData)

        if (Utils.isUnicodeStrEquals(correctAnswer,answer))
          sendCorrectMsg(context,pos, answer)
        else
          sendIncorrectMsg(context,pos, correctAnswer)

      })

  }


  override def validateResult(examinationData: ExaminationData, component: FillInBlank): Boolean = {
    component.validateResult(examinationData.currentFibAnswers)
  }

  override def buildInvalidInputAnswerMessage(context: BotContext): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getInvalidInputFibMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(900),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

  def sendCorrectMsg(context: BotContext, pos: Int, correctAnswer: String): Unit = {
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
        effectId = Some(ZConfig.getString("bot.effect_answer_correct")),
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(1300),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }

  def sendIncorrectMsg(context: BotContext, pos: Int, correctAnswer: String): Unit = {
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(
            s"""${botConfig.getReviewIncorrectMsg()}
               |The correct answer #${pos + 1}: ${correctAnswer}""".stripMargin)
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

}

abstract class BaseMCHandler extends QuestionHandler[Seq[Int],MultiChoice] {

  override def resolveUserAnswer(chatMessage: ChatMessage): Option[Seq[Int]] = {
    chatMessage.text
      .flatMap(x => if (x == null || x.trim.isEmpty) None else Some(x.trim))
      .filter(answerText => answerText.matches("^(\\d+)([\\s,]*\\d+)*$"))
      .map(text => {
        "\\d+".r.findAllMatchIn(text)
          .map(_.group(0))
          .map(_.toInt - 1 )
          .filter(_ >= 0)
          .toSeq
          .distinct
      })
  }

  override def checkPartialAnswerAndShowResult(context: BotContext,
                                               examinationData: ExaminationData,
                                               component: MultiChoice,
                                               userAnswers: Seq[Int]): Unit = {

    examinationData.addMCAnswer(userAnswers)
    context.updateContextData(BotContext.EXAMINATION_DATA, examinationData)
    val isCorrect = component.validateResult(userAnswers)
    if (isCorrect) {
      sendCorrectMsg(context)
    } else {
      sendIncorrectMsg(context,component)
    }
  }


  override def validateResult(examinationData: ExaminationData, component: MultiChoice): Boolean = {
    component.validateResult(examinationData.currentMcAnswers)
  }

  override def isAnswerCompleted(examinationData: ExaminationData, component: MultiChoice): Boolean = {
    examinationData.getCurrentMCAnswerCount() >= component.getCorrectAnswers().size
  }

  def sendCorrectMsg(context: BotContext): Unit = {
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
        effectId = Some(ZConfig.getString("bot.effect_answer_correct")),
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(1000),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )

    context.write(msg)
  }

  def sendIncorrectMsg(context: BotContext, component: MultiChoice): Unit = {
    val body = ListBuffer.empty[Component]
    body.append(
      component.copy(
        question = s"""${botConfig.getReviewIncorrectMsg()}
                      |The correct answers is:""".stripMargin,
        answers = component.getCorrectAnswers()
      ).asInactive()
    )
    val msg = ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = body,
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

  override def buildInvalidInputAnswerMessage(context: BotContext): ChatMessage = {
    ChatMessage.create(
      card = Some(XCard(
        version = 1,
        body = Seq(
          Text(botConfig.getInvalidInputMCMsg())
        ),
        actions = Seq.empty,
        fallbackText = None,
        background = None,
        speak = None,
        suggestions = Seq.empty
      )),
      postDelayMillis = Some(900),
      currentAction = Option(context.actionInfo),
      sender = Some(ChatMessage.KIKIBOT),
      recipient = Option(context.recipient.username)
    )
  }

}



