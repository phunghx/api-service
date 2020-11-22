package xed.chatbot.domain

import xed.api_service.domain.course.CourseInfo
import xed.api_service.service.NLPService
import xed.api_service.util.JsonUtils
import xed.userprofile.SignedInUser

import scala.collection.mutable.ListBuffer

object BotContext {
  val EXAMINATION_DATA = "examination_data"

  val REVIEW_CTX = "review"
  val LEARN_TEST_CTX = "learn_test"
  val LEARN_TEST_FOLLOWCTX = "learn_test-followup"

  val LEARN_VOCABULARY_CTX = "learn_vocabulary"
  val LEARN_VOCABULARY_FOLLOWCTX = "kikilearnvocabulary-followup"

  val LEARN_INTRODUCTION_CTX = "learn_introduction"
  val LEARN_INTRODUCTION_FOLLOWCTX = "kikilearnintroduction-followup"

  val CHALLENGE_CTX = "challenge"
  val CHALLENGE_JOIN_CTX = "challenge_join"
  val CHALLENGE_FOLLOWCTX = "challenge-followup"

  val LEARN_WORD_CTX = "learn_word"

  val SEARCH_DICT_CTX = "dictionary_search"
  val SEARCH_DICT_FOLLOWCTX = "kikidictionarysearch-followup"

  val CHALLENGE_PAGING_CTX = "challenge_paging"
  val CHALLENGE_TEMPLATE_PAGING_CTX = "challenge_template_paging"
  val COURSE_PAGING_CTX = "course_paging"
}

case class BotContext(nlpService: NLPService,
                       recipient: SignedInUser,
                       var actionInfo: IntentActionInfo) {



  private val messages = ListBuffer.empty[ChatMessage]

  def getExaminationData() = {
    getContextParam[ExaminationData](BotContext.EXAMINATION_DATA, ExaminationData(
      cardIds = Seq.empty,
      totalCount = 0,
      currentCardIndex = -1,
      currentFrontIndex = 0))
  }

  def getChallengePagingParam() = {
    getContextParam[PagingData](BotContext.CHALLENGE_PAGING_CTX, PagingData())
  }

  def getChallengeTemplatePagingParam() = {
    getContextParam[PagingData](BotContext.CHALLENGE_TEMPLATE_PAGING_CTX, PagingData())
  }

  def getCoursePagingParam() = {
    getContextParam[PagingData](BotContext.COURSE_PAGING_CTX, PagingData())
  }



  def getSearchDictParam() = {
    getContextParam[DictSearchData](BotContext.SEARCH_DICT_CTX, DictSearchData())
  }

  def updateChallengeContext(challengeData: ChallengeData) = {
    updateContextData(BotContext.CHALLENGE_CTX, challengeData)
    updateContextData(BotContext.CHALLENGE_FOLLOWCTX, EmptyContextData())
  }

  def updateCoursePagingParam(param: PagingData) = {
    updateContextData(BotContext.COURSE_PAGING_CTX, param)
  }

  def updateChallengePagingParam(param: PagingData) = {
    updateContextData(BotContext.CHALLENGE_PAGING_CTX, param)
  }

  def updateChallengeTemplatePagingParam(param: PagingData) = {
    updateContextData(BotContext.CHALLENGE_TEMPLATE_PAGING_CTX, param)
  }

  def getLearnContextData() = {
    getContextParam[LearnData](BotContext.LEARN_VOCABULARY_CTX, LearnData())
  }

  def getLearnIntroductionContextData() = {
    getContextParam[LearnIntroductionData](BotContext.LEARN_INTRODUCTION_CTX, LearnIntroductionData())
  }


  def getChallengeContextData() = {
    getContextParam[ChallengeData](BotContext.CHALLENGE_CTX, ChallengeData(None))
  }


  def getChallengeJoinContextData() = {
    val v = getContextParam[ChallengeJoinData](BotContext.CHALLENGE_JOIN_CTX, ChallengeJoinData(None))
    v.copy(
      challengeId = v.challengeId.flatMap(x => if(x==null || x.isEmpty) None else Some(x))
    )
  }


  def initLearnIntroductionContext(courseInfo: Option[CourseInfo] = None,
                                   courseId: Option[String] = None,
                                   sectionId: Option[String] = None,
                                   topicId: Option[String] = None
                                  ) = {
    updateContextData(BotContext.LEARN_INTRODUCTION_CTX, LearnIntroductionData(
      courseInfo,
      courseId,
      sectionId,
      topicId))
    updateContextData(BotContext.LEARN_INTRODUCTION_FOLLOWCTX, EmptyContextData())
  }



  def getLearnWordParam() = {
    getContextParam[LearnData](BotContext.LEARN_WORD_CTX, LearnData())
  }



  def getContextParam[T: Manifest](ctx: String, default: T) = {
    Option(actionInfo).map(actionInfo => {
      val struct = actionInfo.getOrCreateContext(ctx, nlpService.createContext(recipient.username, _)).getParameters
      JsonUtils.fromStruct[T](struct)
    }).getOrElse(default)
  }

  def updateContextData(ctx: String, param: ContextData) = {
    Option(actionInfo)
      .foreach(_.updateContextData(ctx,param, nlpService.createContext(recipient.username, _)))

  }


  def removeLearnContext() = {
    removeContextParam(BotContext.LEARN_VOCABULARY_CTX)
    removeContextParam(BotContext.LEARN_VOCABULARY_FOLLOWCTX)
  }

  def removeLearnIntroductionContext() = {
    removeContextParam(BotContext.LEARN_INTRODUCTION_CTX)
    removeContextParam(BotContext.LEARN_INTRODUCTION_FOLLOWCTX)
  }

  def removeLearnTestContext() = {
    removeContextParam(BotContext.LEARN_TEST_CTX)
    removeContextParam(BotContext.LEARN_TEST_FOLLOWCTX)
  }


  def removeChallengeContextData(): Unit = {
    removeContextParam(BotContext.CHALLENGE_CTX)
    removeContextParam(BotContext.CHALLENGE_FOLLOWCTX)
    removeContextParam(BotContext.EXAMINATION_DATA)
  }

  def removeSearchDictParam(): Unit = {
    removeContextParam(BotContext.SEARCH_DICT_CTX)
    removeContextParam(BotContext.SEARCH_DICT_FOLLOWCTX)
  }

  def removeContextParam(ctx: String) = {
    Option(actionInfo).foreach(_.removeContext(ctx))
  }

  def getMessages: Seq[ChatMessage] = messages

  def write(message: ChatMessage): BotContext = {
    messages.append(message)
    this
  }
}