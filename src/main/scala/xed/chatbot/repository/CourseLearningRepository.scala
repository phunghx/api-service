package xed.chatbot.repository

import com.typesafe.config.Config
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.course.UserLearningInfo
import xed.api_service.util.JsonUtils
import xed.profiler.Profiler

import scala.collection.JavaConversions._

/**
  * @author andy - andy
  * @since 12/15/19
 **/
trait CourseLearningRepository {


  lazy val clazz = getClass.getSimpleName

  def addLearningInfo(learningInfo: UserLearningInfo): Boolean

  def getLearningInfo(username: String): Option[UserLearningInfo]

  def addLearntCardId(username: String,
                      courseId: String,
                      sessionId: String,
                      topicId: String,
                      cardId: String): Boolean

  def updateLearntCardIds(username: String,
                          courseId: String,
                          sessionId: String,
                          topicId: String,
                          cardIds: Seq[String]): Boolean

  def addCompletedCourse(username: String, courseId: String): Boolean

  def removeCompletedCourse(username: String, courseId: String): Boolean

  def addCompletedSection(username: String, sectionId: String): Boolean

  def addCompletedTopic(username: String, topicId: String): Boolean

  def isCompletedCourse(username: String, courseId: String): Boolean

  def getNotLearnCardIds(username: String,
                         courseId: String,
                         cardIds: Seq[String]): Seq[String]

  def getTopicNotLearnCardIds(username: String,
                              topicId: String,
                              cardIds: Seq[String]): Seq[String]

  def getDoneCardIds(username: String, courseId: String): Seq[String]

  def removeDoneCardIds(username: String,
                        courseId: String,
                        sessionId: String,
                        topicId: String,
                        cardIds: Seq[String]): Boolean

  def getTotalDoneCardByCourse(username: String, courseIds: Seq[String]): Map[String,Int]

  def getTotalDoneCard(username: String, courseId: String): Int

  def getLearningCourses(username: String): Seq[String]

  def isLearningCourse(username: String, courseId: String): Boolean

  def isLearningTopic(username: String, topicId: String) : Boolean

  def isLearningSection(username: String, sectionId: String) : Boolean

  def markLearning(username: String,
                   courseId: Option[String],
                   sectionId: Option[String],
                   topicId: Option[String]): Boolean


}

case class UserCourseLearningRepository(ssdb: SSDB, config: Config)
    extends CourseLearningRepository {

  val key = config.getString("key")

  val learningCourseKey = config.getString("learning_course_key")
  val learningSectionKey = config.getString("learning_section_key")
  val learningTopicKey = config.getString("learning_topic_key")

  val completedCourseKey = config.getString("completed_course_key")
  val completedSessionKey = config.getString("completed_session_key")
  val completedTopicKey = config.getString("completed_topic_key")

  val courseCardKey = config.getString("course_card_key")
  val sessionCardKey = config.getString("section_card_key")
  val topicCardKey = config.getString("topic_card_key")

  override def markLearning(username: String,
                            courseId: Option[String],
                            sectionId: Option[String],
                            topicId: Option[String]): Boolean = {

    val time = System.currentTimeMillis()
    val cli = ssdb.batch()

    courseId.foreach(courseId =>{
      cli.zset(learningCourseKey.format(username), courseId, time)
    })

    sectionId.foreach(sessionId =>{
      cli.zset(learningSectionKey.format(username), sessionId, time)
    })

    topicId.foreach(topicId =>{
      cli.zset(learningTopicKey.format(username), topicId, time)
    })

    cli
      .exec()
      .filter(_.ok())
      .nonEmpty
  }

  override def addLearningInfo(learningInfo: UserLearningInfo): Boolean = {
    val cli = ssdb.batch()
    if (learningInfo.courseId.isDefined) {
      cli.zset(
        learningCourseKey.format(learningInfo.username),
        learningInfo.courseId.get,
        System.currentTimeMillis()
      )
    }
    cli.hset(key, learningInfo.username, JsonUtils.toJson(learningInfo))

    cli
      .exec()
      .filter(_.ok())
      .nonEmpty
  }

  override def getLearningInfo(username: String): Option[UserLearningInfo] = {
    val r = ssdb.hget(key, username)
    if (!r.ok()) None
    else {
      Some(JsonUtils.fromJson[UserLearningInfo](r.asString()))
    }
  }

  override def addLearntCardId(username: String,
                               courseId: String,
                               sectionId: String,
                               topicId: String,
                               cardId: String): Boolean = {
    val time = System.currentTimeMillis()
    val cli = ssdb.batch()

    cli.zset(courseCardKey.format(username, courseId), cardId, time)

    cli.zset(sessionCardKey.format(username, sectionId), cardId, time)

    cli.zset(topicCardKey.format(username, topicId), cardId, time)

    cli
      .exec()
      .filter(_.ok())
      .nonEmpty

  }

  override def updateLearntCardIds(username: String,
                                   courseId: String,
                                   sectionId: String,
                                   topicId: String,
                                   cardIds: Seq[String]): Boolean = {
    val time = System.currentTimeMillis()
    val pairs = cardIds.map(id => id -> time)

    val cli = ssdb.batch()

    cli.multi_zset(courseCardKey.format(username, courseId), pairs: _*)

    cli.multi_zset(sessionCardKey.format(username, sectionId), pairs: _*)

    cli.multi_zset(topicCardKey.format(username, topicId), pairs: _*)

    cli
      .exec()
      .filter(_.ok())
      .nonEmpty
  }

  override def addCompletedCourse(username: String,
                                  courseId: String): Boolean = {
    val r = ssdb.zset(
      completedCourseKey.format(username),
      courseId,
      System.currentTimeMillis()
    )
    r.ok()
  }


  override def removeCompletedCourse(username: String, courseId: String): Boolean = {
    val r = ssdb.zdel(
      completedCourseKey.format(username),
      courseId
    )
    r.ok()
  }

  override def addCompletedSection(username: String,
                                   sectionId: String): Boolean = {
    val r = ssdb.zset(
      completedSessionKey.format(username),
      sectionId,
      System.currentTimeMillis()
    )
    r.ok()
  }

  override def addCompletedTopic(username: String, topicId: String): Boolean = {
    val r = ssdb.zset(
      completedTopicKey.format(username),
      topicId,
      System.currentTimeMillis()
    )
    r.ok()
  }

  override def isCompletedCourse(username: String,
                                 courseId: String): Boolean = {

    val r = ssdb.multi_zget(completedCourseKey.format(username), courseId)

    if (!r.ok()) false
    else {
      r.listString()
        .grouped(2)
        .nonEmpty
    }
  }

  override def getNotLearnCardIds(username: String,
                                  courseId: String,
                                  cardIds: Seq[String]): Seq[String] = Profiler(s"$clazz.getUnCompletedCards") {

    if (cardIds.isEmpty)
      Seq.empty
    else {
      val r =
        ssdb.multi_zget(courseCardKey.format(username, courseId), cardIds: _*)

      val completedItems =
        if (!r.ok()) Set.empty
        else {
          r.listString()
            .grouped(2)
            .map(_.get(0))
            .toSet
        }
      cardIds.filterNot(completedItems.contains(_))
    }
  }

  override def getTopicNotLearnCardIds(username: String,
                                       topicId: String,
                                       cardIds: Seq[String]): Seq[String] = {
    if (cardIds.isEmpty)
      Seq.empty
    else {
      val r =
        ssdb.multi_zget(topicCardKey.format(username, topicId), cardIds: _*)

      val completedItems =
        if (!r.ok()) Set.empty
        else {
          r.listString()
            .grouped(2)
            .map(_.get(0))
            .toSet
        }
      cardIds.filterNot(completedItems.contains(_))
    }
  }

  override def getDoneCardIds(username: String,
                              courseId: String): Seq[String] = {

    val key = courseCardKey.format(username, courseId)

    val total = ssdb.zsize(key).asInt()
    val r = ssdb.zrange(key, 0, total)
    if (!r.ok())
      Seq.empty
    else {
      r.listString()
        .grouped(2)
        .map(_.get(0))
        .toSeq
    }
  }

  override def removeDoneCardIds(username: String,
                                 courseId: String,
                                 sessionId: String,
                                 topicId: String,
                                 cardIds: Seq[String]): Boolean = {


    val cli = ssdb.batch()

    cli.multi_zdel(courseCardKey.format(username, courseId), cardIds: _*)

    cli.multi_zdel(sessionCardKey.format(username, sessionId), cardIds: _*)

    cli.multi_zdel(topicCardKey.format(username, topicId), cardIds: _*)

    cli
      .exec()
      .filter(_.ok())
      .nonEmpty
  }




  override def getTotalDoneCard(username: String, courseId: String): Int = {
    val key = courseCardKey.format(username, courseId)
    val r =  ssdb.zsize(key);
    if(r.ok()) r.asInt()
    else 0
  }

  override def getTotalDoneCardByCourse(username: String, courseIds: Seq[String]): Map[String, Int] = {
    courseIds.map(courseId =>{
      val count = getTotalDoneCard(username, courseId)
      courseId -> count
    }).toMap
  }

  override def getLearningCourses(username: String): Seq[String] = {
    val key = learningCourseKey.format(username)
    val total = ssdb.zsize(key).asInt()
    val r = ssdb.zrange(key, 0, total)
    if (r.ok()) {
      r.listString()
        .grouped(2)
        .map(_.get(0))
        .toSeq
    } else {
      Seq.empty
    }
  }

  override def isLearningCourse(username: String, courseId: String): Boolean = {
    val key = learningCourseKey.format(username)
    val r = ssdb.zget(key, courseId)
   if(r.ok()) {
     !r.notFound()
   }else {
     false
   }
  }

  override def isLearningSection(username: String, sectionId: String): Boolean = {
    val key = learningSectionKey.format(username)
    val r = ssdb.zget(key, sectionId)
    if(r.ok()) {
      !r.notFound()
    }else {
      false
    }
  }

  override def isLearningTopic(username: String, topicId: String): Boolean = {
    val key = learningTopicKey.format(username)
    val r = ssdb.zget(key, topicId)
    if(r.ok()) {
      !r.notFound()
    }else {
      false
    }
  }


}
