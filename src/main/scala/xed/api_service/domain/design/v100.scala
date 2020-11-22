package xed.api_service.domain.design.v100

import java.util.regex.Pattern

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonDeserializer, JsonNode}
import xed.api_service.domain.design.{BgColorConfig, Component, ComponentType, Container, ImageConfig, TextConfig}
import xed.api_service.util.Utils

import scala.collection.mutable.ListBuffer

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Text(text: String,
                textConfig: Option[TextConfig] = None) extends Component {
  override val componentType = ComponentType.Text
}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Image(url: String,
                 text: Option[String]= None,
                 textConfig: Option[TextConfig] = None,
                 imageConfig: Option[ImageConfig] = None) extends Component {
  override val componentType = ComponentType.Image
}
@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Audio(url: String,
                 text: Option[String] = None,
                 textConfig: Option[TextConfig] = None,
                 useSimpleUI: Option[Boolean] = Some(false) ) extends Component {

  override val componentType = ComponentType.Audio
}
@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Video(url: String,
                 text: Option[String]= None,
                 textConfig: Option[TextConfig] = None,
                 useSimpleUI: Option[Boolean] = Some(false)) extends Component {
  override val componentType = ComponentType.Video
}

abstract class BaseMCComponent extends Component {
  val question: String
  val textConfig: Option[TextConfig]
  val answers: Seq[Answer]

  def validateResult(userAnswers: Seq[Int]): Boolean = {
    val correctAnswers = Option(answers).getOrElse(Seq.empty)
      .zipWithIndex
      .filter(_._1.correct.getOrElse(false))
      .map(_._2)

    val correctCount = correctAnswers.size

    userAnswers.intersect(correctAnswers).size == correctCount

  }

  def getCorrectIndices() = {
    Option(answers).getOrElse(Seq.empty)
      .zipWithIndex
      .filter(_._1.correct.getOrElse(false))
      .map(_._2)
  }

  def getCorrectAnswers(): Seq[Answer] = {
    Option(answers).getOrElse(Seq.empty)
      .filter(_.correct.getOrElse(false))
  }



}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class MultiChoice(question: String,
                       textConfig: Option[TextConfig] = None,
                       answers: Seq[Answer]) extends BaseMCComponent {


  override val componentType = ComponentType.MultiChoice


  def asInactive(): BaseMCComponent = {
    InactiveMultiChoice(
      question = question,
      textConfig = textConfig,
      answers = answers
    )
  }

}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class InactiveMultiChoice(question: String,
                       textConfig: Option[TextConfig] = None,
                       answers: Seq[Answer]) extends BaseMCComponent {


  override val componentType = ComponentType.InactiveMultiChoice

}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class MultiSelect(question: String,
                       textConfig: Option[TextConfig] = None,
                       answers: Seq[Answer]) extends BaseMCComponent {

  override val componentType = ComponentType.MultiSelect
}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Answer(text: Option[String] = None,
                  textConfig: Option[TextConfig] = None,
                  imageUrl: Option[String] = None,
                  audioUrl: Option[String] = None,
                  videoUrl: Option[String] = None,
                  correct: Option[Boolean] = Some(false)) extends Component {

  override val componentType = ComponentType.Answer
}

object FillInBlank {
  val pattern = Pattern.compile("[^\\S\\n\\r]*_{3,}(\\s*\\(\\s*\\d+\\s*\\))?[^\\S\\n\\r]*")


  def createBlank(index: Int, answer: String = "___") : String = {
    s" $answer(${index + 1}) "
  }

  def getBlanks(question: String): Seq[(Int, Int)] = {
    Option(question)
      .map(pattern.matcher)
      .map(matcher => {
        val blanks = ListBuffer.empty[(Int, Int)]
        while (matcher.find()) {
          blanks.append((matcher.start(), matcher.end()))
        }
        blanks
      }).getOrElse(ListBuffer.empty)
  }

  def format(originQuestion: String): String = {
    getBlanks(originQuestion) match {
      case blanks if blanks.nonEmpty =>
        blanks.zipWithIndex.foldLeft(("", (0, 0)))((r, item) => {
          val lastBlank = r._2
          val blank = item._1
          val index = item._2

          var question = r._1 + originQuestion.substring(lastBlank._2, blank._1) + createBlank(index)
          if(index == blanks.length - 1) {
            question  = question+ originQuestion.substring(blank._2)
          }
          (question, blank)
        })._1
      case _ => originQuestion
    }
  }

  def formatWithAnswers(originQuestion: String, correctAnswers: Seq[String]): String = {
    val question = format(originQuestion)
    val blanks = getBlanks(question)
    if (blanks != null && blanks.nonEmpty) {
      var r = question.substring(0, blanks(0)._1)
      var i = 0
      while (i < blanks.size && i < correctAnswers.size) {
        val end = blanks(i)._2
        val blankWithAnswer = s" ${correctAnswers(i)} "

        r = r + blankWithAnswer
        if (i < correctAnswers.length - 1) r = r + question.substring(end, blanks(i + 1)._1)
        if (i == correctAnswers.length - 1 && blanks(i)._2 < question.length) r = r + question.substring(blanks(i)._2)

        i += 1
      }
      r
    }
    else if (correctAnswers.nonEmpty) {
      s"$question\n\nAnswer: ${correctAnswers(0)}"
    }
    else question
  }

}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class FillInBlank(question: String,
                       textConfig: Option[TextConfig] = None,
                       correctAnswers: Seq[String]) extends Component {

  override val componentType = ComponentType.FillInBlank


  def getCorrectAnswerCount() =   correctAnswers.size

  def validateResult(userAnswers: Seq[String]) = {
    if(correctAnswers.size != userAnswers.size) false else {
      correctAnswers.zipWithIndex.map(e =>{
        val correctAnswer = e._1
        val userAnswer = userAnswers(e._2).trim

        Utils.isUnicodeStrEquals(correctAnswer,userAnswer)
      }).filterNot(x => x).isEmpty
    }
  }

  def fillAnswers(answers: Seq[String]): FillInBlank = {
    copy(
      question = FillInBlank.formatWithAnswers(question,answers)
    )
  }

  def fillCorrectAnswers() = fillAnswers(correctAnswers)

}

@SerialVersionUID(20200602L)
case class Pronunciation(region: String,
                         phoneticTranscription: Option[String],
                         audioUrl: Option[String]) extends  Serializable
@SerialVersionUID(20200602L)
case class Translation(meaning: String,
                       description: Option[String],
                       examples: Seq[PhraseExample]) extends Serializable
@SerialVersionUID(20200602L)
case class PhraseExample(phrase: String,
                         audioUrl: Option[String] = None) extends Serializable

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Dictionary(word: String,
                      partOfSpeech: String,
                      pronunciations: Seq[Pronunciation],
                      translations: Seq[Translation],
                      images: Option[Seq[String]] = None) extends Component {

  override val componentType = ComponentType.Dictionary
}

@SerialVersionUID(20200602L)
@JsonDeserialize(using = classOf[JsonDeserializer.None])
case class Panel(components: Seq[Component],
                 text: Option[String] = None,
                 isHorizontal: Option[Boolean] = Some(false),
                 backgroundColor: Option[BgColorConfig] = None,
                 alignment: Option[Int] = None
                ) extends Container {

  override val componentType = ComponentType.Panel
}



