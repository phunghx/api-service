package xed.chatbot.repository

import com.twitter.util.Future
import education.x.commons.{SsdbKVS, SsdbList, SsdbSortedSet}
import org.nutz.ssdb4j.spi.SSDB
import xed.api_service.domain.exception.{InternalError, NotFoundError}
import xed.api_service.domain.response.PageResult
import xed.api_service.util.Implicits._
import xed.chatbot.domain.challenge.{Challenge, ChallengeTemplate, CreateChallengeTemplateRequest}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


case class ChallengeRepository(ssdb: SSDB) {


  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val templateIdList = SsdbSortedSet("xed.challenge_template_ids", ssdb)
  val templateKvs = SsdbKVS[String, ChallengeTemplate]("xed.challenge_templates", ssdb)

  val globalIdsSS = SsdbSortedSet("xed.global_challenge_ids", ssdb)
  val kvs = SsdbKVS[Int, Challenge]("xed.challenges", ssdb)


  def addTemplate(template: ChallengeTemplate): Future[ChallengeTemplate] = {
    for {
      _ <- templateIdList.add(template.templateId, System.currentTimeMillis()).asTwitter
      r <- templateKvs.add(template.templateId, template).asTwitter
    } yield r match {
      case true => template
      case _ => throw InternalError("Can't add this template.");
    }
  }

  def setQuestionIdList(templateId: String, questionIds: Seq[String]): Future[Option[String]] = {
    val questionListId = s"xed.challenge.question.$templateId"
    val questionList = SsdbList[String](questionListId, ssdb)
    for {
      _ <- questionList.clear().asTwitter
      r <- questionList.multiPushBack(questionIds.toArray).asTwitter
    } yield r match {
      case true => Option(questionListId)
      case _ => None
    }
  }

  def createTemplate(username: String,
                     templateId: String,
                     request: CreateChallengeTemplateRequest) : Future[ChallengeTemplate] = {

    for {
      questionListId <- setQuestionIdList(templateId,request.questionIds)
        .map({
            case None => throw InternalError("Can't create question list for this template.")
            case Some(questionListId) => questionListId
          })
      template = ChallengeTemplate(
        templateId = templateId,
        challengeType = Some(request.challengeType),
        name = Some(request.name),
        description = request.description,
        canExpired = request.canExpired,
        duration = request.duration,
        questionListId = questionListId
      )
      _ <- templateIdList.add(templateId, System.currentTimeMillis()).asTwitter
      r <- templateKvs.add(templateId, template).asTwitter
    } yield r match {
      case true => template
      case _ => throw InternalError("Can't create this template.");
    }
  }

  def getChallengeTemplates(from: Int, size: Int): Future[PageResult[ChallengeTemplate]] = {
    for {
      total <- templateIdList.size().map(_.getOrElse(0)).asTwitter
      templateIds <- templateIdList.range(from,size)
        .map(_.getOrElse(Array.empty))
        .map(_.map(_._1))
        .asTwitter
      data <- templateKvs.multiGet(templateIds)
        .asTwitter
          .map(_.getOrElse(Map.empty))
    } yield {
      val records = templateIds.map(data.get)
        .filter(_.isDefined)
        .map(_.get)
      PageResult(total, records)
    }
  }


  def deleteChallengeTemplate(templateId: String): Future[Boolean] = {
    for {
      _ <- templateIdList.remove(templateId).asTwitter
      r <- templateKvs.remove(templateId).asTwitter
    } yield {
      r
    }
  }

  def getChallengeTemplate(templateId: String) : Future[Option[ChallengeTemplate]] = {
    templateKvs.get(templateId).asTwitter
  }

  def addGlobalChallenge(challengeId: Int) : Future[Boolean] = {
    globalIdsSS.add(
      challengeId.toString,
      System.currentTimeMillis()
    ).asTwitter
  }

  def removeGlobalChallenge(challengeId: Int) : Future[Boolean] = {
    globalIdsSS.remove(
      challengeId.toString
    ).asTwitter
  }

  def getGlobalChallengeIds() = {
    for {
      size <- globalIdsSS.size().asTwitter.map(_.getOrElse(0))
      r <- globalIdsSS.range(0, size,reverseOrder = true)
        .map(_.getOrElse(Array.empty))
        .asTwitter
    } yield {
      (r.map(arr => (arr._1.toInt, arr._2)).toSeq, size)
    }
  }


  def addChallenge(challenge: Challenge): Future[Boolean] = {
    kvs.add(challenge.challengeId, challenge).asTwitter
  }

  def updateChallenge(challenge: Challenge): Future[Boolean] = addChallenge(challenge)

  def getChallenge(challengeId: Int): Future[Challenge] = {
    kvs.get(challengeId).asTwitter.map({
      case Some(x) => x
      case _ => throw NotFoundError(Some("this challenge isn't found."))
    })
  }

  def getChallengeAsMap(challengeIds: Seq[Int]): Future[Map[Int, Challenge]] = {
    kvs.multiGet(challengeIds.toArray).asTwitter.map({
      case Some(x) => x
      case _ => Map.empty
    })
  }

  def getQuestionCount(questionListId: String): Future[Int] = {
    val questionList = SsdbList[String](questionListId, ssdb)
    questionList.size()
      .asTwitter
      .map(_.getOrElse(0))
  }


  def getQuestionIds(questionListId: String) : Future[Seq[String]] = {
    val questionList = SsdbList[String](questionListId, ssdb)
    questionList.getAll().asTwitter.map({
      case Some(x) => x.toSeq
      case _ => Seq.empty
    })
  }
}




