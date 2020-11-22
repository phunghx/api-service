package xed.chatbot.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import xed.api_service.domain.SRSCard
import xed.api_service.domain.course.CourseInfo
import xed.api_service.domain.design.Container
import xed.api_service.util.JsonUtils
import xed.chatbot.domain.challenge.Challenge

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait ContextData {
  final def asStruct(): Struct = {
    val builder = Struct.newBuilder()
    JsonFormat
      .parser()
      .merge(JsonUtils.toJson(this),builder)

    val struct = builder.build()
    struct

  }
}

case class EmptyContextData() extends ContextData

case class ExaminationData(cardIds: Seq[String],
                           totalCount: Int,
                           var currentCardIndex: Int,
                           var currentFrontIndex: Int,
                           var currentFibAnswers: ListBuffer[String] = ListBuffer.empty,
                           var currentMcAnswers: ListBuffer[Int] = ListBuffer.empty,
                           var correctCardIds: mutable.HashSet[String] = mutable.HashSet.empty,
                           var incorrectCardIds: mutable.HashSet[String] = mutable.HashSet.empty,
                           var skipCardIds: mutable.HashSet[String] = mutable.HashSet.empty,
                           var beginTime : Long = System.currentTimeMillis(),
                           var totalDuration: Long = 0,
                           var lastQuestionBeginTime: Long = System.currentTimeMillis()) extends ContextData {


  def resetForNewQuestion() = {
    if (correctCardIds == null) correctCardIds = mutable.HashSet.empty[String]
    if (incorrectCardIds == null) incorrectCardIds = mutable.HashSet.empty[String]
    if (skipCardIds == null) skipCardIds = mutable.HashSet.empty[String]

    if (currentFibAnswers == null) currentFibAnswers = ListBuffer.empty[String]
    if (currentMcAnswers == null) currentMcAnswers = ListBuffer.empty[Int]

    currentFibAnswers.clear()
    currentMcAnswers.clear()
    lastQuestionBeginTime = System.currentTimeMillis()
  }


  def setCurrentCard(index: Int,
                     frontIndex: Int,
                     reviewCard: Option[SRSCard],
                     front: Option[Container]) = {
    resetForNewQuestion()
    currentCardIndex = index
    currentFrontIndex = frontIndex

  }



  def addFIBAnswer(answer: String) = {
    currentFibAnswers = Option(currentFibAnswers).getOrElse(ListBuffer.empty)
    currentFibAnswers.append(answer)
  }

  def addMCAnswer(answers: Seq[Int]) = {
    currentMcAnswers = Option(currentMcAnswers).getOrElse(ListBuffer.empty)
    currentMcAnswers.appendAll(answers)
    currentMcAnswers = currentMcAnswers.distinct
  }


  def addFinalResult(cardId: String,
                     isCorrect: Boolean,
                     isSkipped: Boolean = false,
                     duration: Int = 0 ) = {
    correctCardIds = Option(correctCardIds).getOrElse( mutable.HashSet.empty[String])
    incorrectCardIds = Option(incorrectCardIds).getOrElse( mutable.HashSet.empty[String])
    skipCardIds = Option(skipCardIds).getOrElse( mutable.HashSet.empty[String])

    if (currentFibAnswers == null) currentFibAnswers = ListBuffer.empty[String]
    if (currentMcAnswers == null) currentMcAnswers = ListBuffer.empty[Int]

    if(isSkipped) {
      skipCardIds.add(cardId)
      correctCardIds.remove(cardId)
      incorrectCardIds.remove(cardId)
    } else {
      skipCardIds.remove(cardId)
      if(isCorrect) {
        correctCardIds.add(cardId)
        incorrectCardIds.remove(cardId)
      }
      else {
        incorrectCardIds.add(cardId)
        correctCardIds.remove(cardId)
      }
    }
    totalDuration += duration
    currentFibAnswers.clear()
    currentMcAnswers.clear()
    lastQuestionBeginTime = System.currentTimeMillis()


  }


  @JsonIgnore
  def getDuration() = {
    val delta = (System.currentTimeMillis() - lastQuestionBeginTime).toInt
    if(delta > 0) delta else 0
  }

  @JsonIgnore
  def getCardId() = {
    val index = currentCardIndex
    (cardIds lift index) match {
      case Some(cardId) =>
        Some(cardId)
      case _ => None
    }
  }

  @JsonIgnore
  def getNextCardId() = {
    val index = currentCardIndex + 1
    (cardIds lift index) match {
      case Some(cardId) => Some(cardId)
      case _ => None
    }
  }

  @JsonIgnore
  def getPoints(): Int = {
    val numberOfQuestion = getCorrectQuestionCount()

    numberOfQuestion*1
  }

  @JsonIgnore
  def getCorrectQuestionCount() =  Option(correctCardIds).map(_.size).getOrElse(0)

  @JsonIgnore
  def getIncorrectQuestionCount() =  Option(incorrectCardIds).map(_.size).getOrElse(0)

  @JsonIgnore
  def getSkipQuestionCount() =  Option(skipCardIds).map(_.size).getOrElse(0)

  @JsonIgnore
  def getTotalAnsweredQuestions() = getCorrectQuestionCount() + getIncorrectQuestionCount() + getSkipQuestionCount()

  @JsonIgnore
  def getCurrentFIBAnswerCount() = Option(currentFibAnswers).map(_.size).getOrElse(0)

  @JsonIgnore
  def getCurrentMCAnswerCount() = Option(currentMcAnswers).map(_.size).getOrElse(0)

}

object PagingData{
  val DEFAULT_SIZE = 10
}

case class PagingData(totalItemCount: Double = 0.0,
                      from: Option[Double] = Some(0.0),
                      size : Option[Double] = Some(PagingData.DEFAULT_SIZE.toDouble),
                      ids: Option[Seq[String]] = None) extends ContextData {

  def canPaging(isNext: Boolean): Boolean = {
    val totalItems = totalItemCount

    val fromIndex = if(isNext)
      from.getOrElse(0.0) + size.getOrElse(PagingData.DEFAULT_SIZE.toDouble)
    else
      from.getOrElse(0.0) - size.getOrElse(PagingData.DEFAULT_SIZE.toDouble)

    if(fromIndex >= totalItems || fromIndex < 0)
      false
    else
      true
  }



}

case class LearnData(var learnCardId: Option[String] = None) extends ContextData {


  def setCardToLearn(cardId: String): Unit = {
    learnCardId = Option(cardId)
  }

  def getCurrentCardId() = {
    learnCardId.headOption
  }

}

case class LearnIntroductionData(courseInfo: Option[CourseInfo] = None,
                                 courseId: Option[String] = None,
                                 sectionId: Option[String] = None,
                                 topicId: Option[String] = None
                                ) extends ContextData {
}

case class ChallengeJoinData(challengeId: Option[String]) extends ContextData {

}

case class ChallengeData(challengeId: Option[String],
                         challenge: Option[Challenge] = None) extends ContextData {

}

case class DictSearchData(word: Option[String] = None,
                          targetLang: Option[String] = None) extends ContextData