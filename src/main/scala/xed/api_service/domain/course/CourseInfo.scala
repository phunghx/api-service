package xed.api_service.domain.course

import xed.api_service.repository.ElasticsearchObject
import xed.api_service.util.JsonUtils
import xed.userprofile.domain.ShortUserProfile

import scala.collection.mutable.ListBuffer

object LanguageLevel {

  val Elementary = "elementary"
  val Intermediate = "intermediate"
  val UpperIntermediate = "upper_intermediate"
  val Advanced = "advanced"

  private val levelIds =
    Seq(Elementary, Intermediate, UpperIntermediate, Advanced)

  private val nameMap = Map(
    Elementary -> "Elementary",
    Intermediate -> "Intermediate",
    UpperIntermediate -> "High Intermediate",
    Advanced -> "Advanced"
  )

  def getLevelName(levelId: String) = {
    nameMap.get(levelId).getOrElse("Elementary")
  }

  def getNextLevelId(completedLevelId: String) = {
    val index = levelIds.indexOf(completedLevelId)
    if (index < 0) None
    else {
      levelIds.drop(index + 1).headOption
    }
  }

}

case class LearningCourseInfo(id: String,
                              totalSection: Int,
                              totalTopic: Int,
                              totalCard: Int,
                              completedCard: Int,
                              learningCard: Int,
                              level: Option[String],
                              name: Option[String],
                              thumbnail: Option[String],
                              description: Option[String],
                              var creatorId: Option[String],
                              var creator: Option[ShortUserProfile] = None)

case class CourseInfo(id: String,
                      level: Option[String],
                      name: Option[String],
                      thumbnail: Option[String],
                      description: Option[String],
                      journeyIds: Option[ListBuffer[String]],
                      deckIds: Option[Seq[String]],
                      totalCard: Option[Int],
                      status: Option[Int],
                      creator: Option[String],
                      var updatedTime: Option[Long],
                      var createdTime: Option[Long],
                      var creatorDetail: Option[ShortUserProfile] = None)
    extends ElasticsearchObject {

  override def esId: String = id

  override def esSource: String = JsonUtils.toJson(this)

}

case class LearnCard(courseId: String,
                     sectionId: String,
                     topicId: String,
                     cardId: String)

case class UserLearningInfo(username: String,
                            courseId: Option[String],
                            cardIds: Option[Seq[LearnCard]] = None,
                            var knownIndex: Option[Int] = Some(0),
                            var currentIndex: Option[Int] = Some(0),
                            var dontKnowIndex: Option[Int] = Some(0)) {
  def allLearningCardIds(): Seq[String] = {
    cardIds
      .getOrElse(Seq.empty)
      .map(_.cardId)
  }

  def isCourseRequired = courseId.isEmpty

  def getLearningCardId(): Option[LearnCard] = {
    val (l, index, r, max) = getLearningRange()

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(0)

    cardIds.getOrElse(Seq.empty) lift currentIndex.getOrElse(0)
  }

  def removeCurrentAndNext(known: Boolean): UserLearningInfo = {

    val (l, index, r, max) = getLearningRange()

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(0)

    val x = this.copy(cardIds = cardIds.map(x => {
      val newList = ListBuffer(x: _*)
      newList.remove(index)
      newList
    }))
    x.suggestNextCard(known)
    x
  }

  private def getLearningRange(): (Int, Int, Int, Int) = {
    val max = cardIds match {
      case Some(x) if x.nonEmpty => x.size - 1
      case _                     => 0
    }
    val l = knownIndex.getOrElse(0)
    val r = math.min(
      dontKnowIndex.getOrElse(cardIds.map(_.size - 1).getOrElse(0)),
      max
    )
    val index = math.min(currentIndex.getOrElse(0), max)

    (l, index, r, max)
  }

  def initSuggestionRange(): Unit = {
    val l = 0
    val r = cardIds match {
      case Some(x) if x.nonEmpty => x.size - 1
      case _                     => 0
    }
//    val index = if(r > l) (l + (r - l) / 2) else 0
    val index = 0

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(index)
  }

  def updateSuggestionRange(): Unit = {

    val (l, index, r, max) = getLearningRange()

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(0)
  }

  def suggestNextCard(known: Boolean): Unit = {
    if (known) knowCurrentCard()
    else
      dontknowCurrentCard()
  }

  private def knowCurrentCard(): Unit = {
    val max = cardIds match {
      case Some(x) if x.nonEmpty => x.size - 1
      case _                     => 0
    }
    var l = knownIndex.getOrElse(0)
    var r = math.min(
      dontKnowIndex.getOrElse(cardIds.map(_.size - 1).getOrElse(0)),
      max
    )
    var index = math.min(currentIndex.getOrElse(0), max)

    if (index == max) {
      l = 0
      r = max
      println("Reset range")
    } else {
      l = index
      if (l == r) {
        r = max
      }
    }

    index = if (r > l) (l + (r - l) / 2) else 0

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(0)

  }

  private def dontknowCurrentCard(): Unit = {

    val max = cardIds match {
      case Some(x) if x.nonEmpty => x.size - 1
      case _                     => 0
    }
    var l = knownIndex.getOrElse(0)
    var r = math.min(
      dontKnowIndex.getOrElse(cardIds.map(_.size - 1).getOrElse(0)),
      max
    )
    var index = math.min(currentIndex.getOrElse(0), max)

    if (index >= r) {
      println("Reset range")
      l = r
      r = max
      index = if (r > l) (l + (r - l) / 2) else 0
    } else if (index >= max) {
      l = 0
      r = max
      index = if (r > l) (l + (r - l) / 2) else 0
    } else {
      index += 1
    }

    knownIndex = Some(l)
    dontKnowIndex = Some(r)
    currentIndex = Some(0)

  }

  override def toString: String = {
    s"""
       |Total cards: ${cardIds.map(_.size).getOrElse(0)}
       |Known index: $knownIndex
       |Current index: $currentIndex
       |DontKnown index: $dontKnowIndex
       |Card id: ${(cardIds.getOrElse(Seq.empty) lift currentIndex.getOrElse(0))
         .getOrElse("")}
       |""".stripMargin
  }
}
