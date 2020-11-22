package xed.chatbot.service

import com.twitter.util.Future
import xed.api_service.domain.course.CourseInfo
import xed.api_service.domain.exception.{InternalError, NotFoundError}
import xed.api_service.domain.response.PageResult
import xed.api_service.service.course.CourseService
import xed.api_service.service.{CardService, DeckService}
import xed.api_service.util.Utils
import xed.chatbot.domain.challenge.{ChallengeTemplate, CreateChallengeTemplateFromCourseRequest, CreateChallengeTemplateRequest}
import xed.chatbot.repository.ChallengeRepository

import scala.collection.mutable.ListBuffer

/**
 * @author andy
 * @since 2/28/20
 **/
trait ChallengeTemplateService {

  def deleteChallengeTemplate(templateId: String): Future[Boolean]

  def getChallengeTemplate(templateId: String): Future[ChallengeTemplate]

  def addTemplate(template: ChallengeTemplate) : Future[ChallengeTemplate]

  def createTemplate(username: String, request: CreateChallengeTemplateRequest) : Future[ChallengeTemplate]

  def createChallengeTemplateFromCourse(username: String, request: CreateChallengeTemplateFromCourseRequest) : Future[ChallengeTemplate]

  def searchChallengeTemplates(from: Int, size: Int) : Future[PageResult[ChallengeTemplate]]

  def setQuestionList(templateId: String, questionIds: Seq[String]): Future[String]

  def getQuestionIds(questionIdList: String): Future[Seq[String]]

  def getQuestionCount(questionIdList: String): Future[Int]



}


case class ChallengeTemplateServiceImpl(repository: ChallengeRepository,
                                courseService: CourseService,
                                deckService: DeckService,
                                cardService: CardService,
                                idGenService: IdGenService) extends ChallengeTemplateService {

  override def createChallengeTemplateFromCourse(username: String, request: CreateChallengeTemplateFromCourseRequest): Future[ChallengeTemplate] = {

    def getValidQuestionIds(courseInfo: CourseInfo, questionCount: Option[Int]) = {
      for {
        cardIds <- courseService.getCardIds(courseInfo)
        questionIds = questionCount.getOrElse(0) match {
          case count if count <= 0 => Utils.getRandomSubset(cardIds, cardIds.size)
          case count => Utils.getRandomSubset(cardIds, count)
        }
        validQuestionIds <- cardService.getQACardIds(questionIds)
      } yield {
        validQuestionIds
      }
    }

    for {
      courseInfo <- courseService.get(request.courseId).map(Utils.throwIfNotExist(_, Some(s"Course not found: ${request.courseId}")))
      questionIds <- getValidQuestionIds(courseInfo, request.questionCount)
      r <- createTemplate(username, CreateChallengeTemplateRequest(
        name = if (request.name.isDefined) request.name.get else courseInfo.name.getOrElse(""),
        description = if (request.description.isDefined) request.description else courseInfo.description,
        challengeType = request.challengeType,
        canExpired = request.canExpired,
        duration = request.duration,
        questionIds = questionIds
      ))
    } yield {
      r
    }
  }

  override def createTemplate(username: String, request: CreateChallengeTemplateRequest): Future[ChallengeTemplate] = {
    for {
      templateId <- idGenService.genChallengeTemplateId()
      template <- repository.createTemplate(username, templateId, request)
    } yield {
      template
    }
  }

  override def addTemplate(template: ChallengeTemplate): Future[ChallengeTemplate] = {
    repository.addTemplate(template)
  }

  override def deleteChallengeTemplate(templateId: String): Future[Boolean] = {
    repository.deleteChallengeTemplate(templateId)
  }

  override def getChallengeTemplate(templateId: String): Future[ChallengeTemplate] = {
    repository.getChallengeTemplate(templateId).map({
      case Some(template) => template
      case _ => throw NotFoundError(Some(s"This template ${templateId} isn't found."))
    })
  }

  override def searchChallengeTemplates(from: Int, size: Int): Future[PageResult[ChallengeTemplate]] = {
    repository.getChallengeTemplates(from, size)
  }

  override def getQuestionIds(questionIdList: String): Future[Seq[String]] = {
    repository.getQuestionIds(questionIdList)
  }

  override def getQuestionCount(questionIdList: String): Future[Int] = {
    repository.getQuestionCount(questionIdList)
  }

  override def setQuestionList(templateId: String, questionIds: Seq[String]): Future[String] = {
    repository.setQuestionIdList(templateId, questionIds).map({
      case Some(id) => id
      case _ => throw InternalError(Some(s"Can't set question list for template $templateId"))
    })
  }

}
