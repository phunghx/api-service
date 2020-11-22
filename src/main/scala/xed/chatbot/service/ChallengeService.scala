package xed.chatbot.service

import com.twitter.util.Future
import xed.api_service.domain.course.CourseInfo
import xed.api_service.domain.exception.InternalError
import xed.api_service.domain.response.PageResult
import xed.api_service.domain.{Deck, Status}
import xed.api_service.service.course.CourseService
import xed.api_service.service.{CardService, DeckService}
import xed.api_service.util.Utils
import xed.chatbot.domain.challenge.{Challenge, CreateChallengeFromCourseRequest, CreateChallengeFromDeckRequest, CreateChallengeRequest, UpdateChallengeFromDeckRequest}
import xed.chatbot.repository.{ChallengeHistoryRepository, ChallengeRepository}

import scala.collection.mutable.ListBuffer

/**
 * @author andy
 * @since 2/28/20
 **/


trait JoinValidator {

  def validate(username: String, challenge: Challenge) : Future[Unit]
}

case class ChallengeValidator(validators: Seq[JoinValidator]) extends JoinValidator {

  override def validate(username: String, challenge: Challenge): Future[Unit] = {
    validators.foldLeft(Future.Unit)((fn,validator) =>{
      fn.flatMap(_ => validator.validate(username, challenge))
    })
  }
}

case class TimeUpValidator() extends JoinValidator {

  override def validate(username: String, challenge: Challenge): Future[Unit] = Future {
    if(challenge.isAlreadyFinished()) {
      throw InternalError(Some("This challenge is already finished."))
    }
  }
}

case class OnceTryOnlyValidator() extends JoinValidator {

  override def validate(username: String, challenge: Challenge): Future[Unit] = Future {

  }
}


trait ChallengeService {

  def createChallengeFromTemplate(username: String, templateId: String) : Future[Challenge]

  def createChallengeFromCourse(username: String, request: CreateChallengeFromCourseRequest) : Future[Challenge]

  def createChallengeFromDeck(username: String, request: CreateChallengeFromDeckRequest) : Future[Challenge]

  def updateChallengeFromDeck(challengeId: Int, request: UpdateChallengeFromDeckRequest) : Future[Challenge]

  def createChallenge(username: String, request: CreateChallengeRequest) : Future[Challenge]

  def joinChallenge(username: String, challengeId: Int): Future[Challenge]

  def updateChallengeInfo(challenge: Challenge) : Future[Boolean]

  def getChallengeInfo(challengeId: Int) : Future[Challenge]

  def setQuestionList(templateId: String, questionIds: Seq[String]): Future[String]

  def getQuestionIds(questionIdList: String): Future[Seq[String]]

  def getQuestionCount(questionIdList: String): Future[Int]

  def getMyChallenges(username: String) : Future[Seq[Challenge]]

  def getMyOpeningChallenges(username: String) : Future[Seq[Challenge]]

  def getMyChallenges(username: String, from: Int, size: Int ) : Future[PageResult[Challenge]]


}

case class ChallengeServiceImpl(repository: ChallengeRepository,
                                challengeHistoryRepository: ChallengeHistoryRepository,
                                watcher: ChallengeWatcher,
                                leaderBoardService: LeaderBoardService,
                                courseService: CourseService,
                                deckService: DeckService,
                                cardService: CardService,
                                idGenService: IdGenService) extends ChallengeService {

  private val joinValidator: JoinValidator = ChallengeValidator(
    validators = Seq(
      TimeUpValidator(),
      OnceTryOnlyValidator()
    )
  )

  override def createChallengeFromCourse(username: String, request: CreateChallengeFromCourseRequest): Future[Challenge] = {
    def getValidQuestionIds(courseInfo: CourseInfo) = {
      for {
        cardIds <- courseService.getCardIds(courseInfo)
        questionIds =  Utils.getRandomSubset(cardIds, cardIds.size)
        validQuestionIds <- cardService.getQACardIds(questionIds)
      } yield {
        validQuestionIds
      }
    }

    for {
      challengeId <- idGenService.genChallengeId()
      courseInfo <- courseService.get(request.courseId).map(Utils.throwIfNotExist(_, Some(s"Course not found: ${request.courseId}")))
      questionIds <- getValidQuestionIds(courseInfo)
      questionIdList <- setQuestionList(courseInfo.id, questionIds)
      challenge = request.toChallenge(username,challengeId, courseInfo, questionIdList)
      createOK <- repository.addChallenge(challenge)
      _ <- if (request.addToGlobal.getOrElse(true)) {
        repository.addGlobalChallenge(challenge.challengeId)
      } else repository.removeGlobalChallenge(challenge.challengeId)
      _ <- challengeHistoryRepository.joinChallenge(username, challenge.challengeId)
      _ <- leaderBoardService.initialPointIfNotFound(username, challengeId.toString)
      _ <- watcher.watch(challenge)
    } yield createOK match {
      case true => challenge
      case _ => throw  InternalError(Some("Can't create challenge from this course at this time"))
    }
  }


  override def createChallengeFromDeck(username: String, request: CreateChallengeFromDeckRequest): Future[Challenge] = {

    for {
      challengeId <- idGenService.genChallengeId()
      deck <- deckService.getDeck(request.deckId)
      cards <- cardService.getQACards(deck.cards.getOrElse(ListBuffer.empty))
      questionIdList <- setQuestionList(deck.id, cards.map(_.id))
      challenge = request.buildChallenge(username,challengeId, deck, questionIdList)
      createOK <- repository.addChallenge(challenge)
      _ <- if (request.addToGlobal.getOrElse(true)) {
        repository.addGlobalChallenge(challenge.challengeId)
      } else repository.removeGlobalChallenge(challenge.challengeId)
      _ <- challengeHistoryRepository.joinChallenge(username, challenge.challengeId)
      _ <- leaderBoardService.initialPointIfNotFound(username, challengeId.toString)
      _ <- watcher.watch(challenge)
    } yield createOK match {
      case true => challenge
      case _ => throw  InternalError(Some("Can't create challenge from this deck at this time"))
    }
  }

  override def updateChallengeFromDeck(challengeId: Int, request: UpdateChallengeFromDeckRequest): Future[Challenge] = {


    for {
      challenge <- getChallengeInfo(challengeId)
      deck <- deckService.getDeck(request.deckId)
      cards <- cardService.getQACards(deck.cards.getOrElse(ListBuffer.empty))
      questionIdList <- setQuestionList(deck.id, cards.map(_.id))
      newChallenge = challenge.copy(
        challengeType = Some(request.challengeType),
        name = request.name match {
          case Some(x) => Some(x)
          case _ => deck.name
        },
        description = request.description match {
          case Some(x) => Some(x)
          case _ => deck.description
        },
        canExpired = request.canExpired,
        questionListId = questionIdList,
        isFinished = Some(false),
        dueTime = request.duration match {
          case Some(x) => Some(challenge.createdTime.getOrElse(System.currentTimeMillis()) + x)
          case _ => challenge.dueTime
        }
      )
      createOK <- repository.addChallenge(newChallenge)
      _ <- if (request.addToGlobal.getOrElse(true)) {
        repository.addGlobalChallenge(newChallenge.challengeId)
      } else
        repository.removeGlobalChallenge(newChallenge.challengeId)
      _ <- watcher.watch(newChallenge)
    } yield createOK match {
      case true => newChallenge
      case _ => throw  InternalError(Some("Can't update challenge from this deck at this time"))
    }
  }


  override def createChallengeFromTemplate(username: String, templateId: String): Future[Challenge] = {
    for {
      template <- repository.getChallengeTemplate(templateId)
        .map(Utils.throwIfNotExist(_, Some("This template is not found.")))
      challenge <- createChallenge(username, CreateChallengeRequest(
        template.name,
        template.description,
        template.challengeType,
        questionIdList = template.questionListId,
        duration = template.duration,
        canExprired = template.canExpired
      ))
    } yield {
      challenge
    }
  }

  override def createChallenge(username: String, request: CreateChallengeRequest): Future[Challenge] = {
    for {
      challengeId <- idGenService.genChallengeId()
      challenge = Challenge(
        challengeId = challengeId,
        challengeType = Some(request.challengeType),
        name = Some(request.name),
        description = request.description,
        canExpired = request.canExprired,
        creator = Some(username),
        questionListId = request.questionIdList,
        status = Some(request.status.getOrElse(Status.PROTECTED.id)),
        isFinished = Some(false),
        createdTime = Some(System.currentTimeMillis()),
        dueTime = request.duration.map(x => System.currentTimeMillis() + x)
      )

      createOK <- repository.addChallenge(challenge)
      _ <- watcher.watch(challenge)
    } yield createOK match {
      case true => challenge
      case _ => throw InternalError(Some("Can't create challenge at this time"))
    }
  }

  override def updateChallengeInfo(challenge: Challenge): Future[Boolean] = {
    repository.updateChallenge(challenge)
  }

  override def joinChallenge(username: String, challengeId: Int): Future[Challenge] = {
    for {
      challenge <- repository.getChallenge(challengeId)
      _ <- joinValidator.validate(username, challenge)
      joinOK <- challengeHistoryRepository.joinChallenge(username, challenge.challengeId)
    } yield joinOK match {
      case true => challenge
      case _ => throw InternalError(Some(s"Can't join challenge #$challengeId"))
    }
  }

  override def getChallengeInfo(challengeId: Int): Future[Challenge] = {
    for{
      challenge <- repository.getChallenge(challengeId)
      questionCount <- repository.getQuestionCount(challenge.questionListId)
    } yield {
      challenge.copy(questionCount = Some(questionCount))
    }
  }

  override def getMyChallenges(username: String): Future[Seq[Challenge]] = {
    for {
      (globalIds, _) <- repository.getGlobalChallengeIds()
      (joinedIds, _) <- challengeHistoryRepository.getJoinChallengeIds(username)
      challengeIds = (globalIds++ joinedIds).sortBy(_._2)(Ordering[Long].reverse).map(_._1).distinct
      challengeMap <- repository.getChallengeAsMap(challengeIds)
    } yield {
      challengeIds.map(challengeMap.get(_))
        .filter(_.isDefined)
        .map(_.get)
    }
  }

  override def getMyOpeningChallenges(username: String): Future[Seq[Challenge]] = {
    for {
      (globalIds, _) <- repository.getGlobalChallengeIds()
      (joinedIds, _) <- challengeHistoryRepository.getJoinChallengeIds(username)
      challengeIds = (globalIds++ joinedIds).sortBy(_._2)(Ordering[Long].reverse).map(_._1).distinct
      challengeMap <- repository.getChallengeAsMap(challengeIds)
    } yield {
      challengeIds.map(challengeMap.get(_))
        .filter(_.isDefined)
        .map(_.get)
        .filterNot(_.isAlreadyFinished())
    }
  }

  override def getMyChallenges(username: String, from: Int, size: Int): Future[PageResult[Challenge]] = {
    for {
      (globalIds, globalCount) <- repository.getGlobalChallengeIds()
      (joinedIds, joinCount) <- challengeHistoryRepository.getJoinChallengeIds(username)
      challengeIds = (globalIds ++ joinedIds).sortBy(_._2)(Ordering[Long].reverse).map(_._1).distinct
      sliceChallengeIds = challengeIds.slice(from, size)
      challengeMap <- repository.getChallengeAsMap(sliceChallengeIds)
    } yield {
      val records = sliceChallengeIds.map(challengeMap.get(_))
        .filter(_.isDefined)
        .map(_.get)
      PageResult(globalCount+ joinCount, records)
    }
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
