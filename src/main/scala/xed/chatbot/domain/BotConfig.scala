package xed.chatbot.domain

import xed.chatbot.domain.BotConfig.{ActionConfig, rnd}
import xed.chatbot.domain.challenge.Challenge
import xed.chatbot.domain.leaderboard.LeaderBoardItem

import scala.util.Random


object BotConfig {

  val rnd = new Random()

  case class ActionConfig(title: String, value: String) {
    def isMatch(x: String) = value.trim.equalsIgnoreCase(x.trim)
  }

}

case class BotConfig(unknownErrorMessages: Seq[String],
                     unrecognizedMessages: Seq[String],
                     review: ReviewBotConfig,
                     challenge: ChallengeBotConfig,
                     action: Map[String, ActionConfig],
                     helpMessages: Seq[String],
                     noCourseMessages: Seq[String],
                     afterReviewSuggestedMessages: Seq[String],
                     learnWordCompletedExplainMessages: Seq[String],
                     challengeListingMessages: Seq[String],
                     courseListingMessages: Seq[String],
                     listingItemSize: Int,
                     askToLearnWordMessages: Seq[String],
                     noWordToLearnMessages: Seq[String],
                     outOfIndexMessages: Seq[String],
                     courseIntroducedMessages: Seq[String],
                     sectionIntroducedMessages: Seq[String],
                     courseCompletedMessages: Seq[String],
                     topicIntroducedMessages: Seq[String],
                     topicCompletedMessages: Seq[String],
                     suggestLearnMessages: Seq[String],
                     dictionaryNotFoundMessages: Seq[String],
                     dictionaryInvalidPosMessages: Seq[String],
                     challengeIntroMessages: Seq[String],
                     challengeStreakingIntroMessages: Seq[String]
                    ) {

  def getErrorMsg(): String = {

    val count = if (unknownErrorMessages.size > 0) unknownErrorMessages.size else 1

    (unknownErrorMessages lift (rnd.nextInt(count))).getOrElse("Something went wrong!")
  }

  def getChallengeIntroMessage(challenge: Challenge, questionCount: Int ): String = {

    val count = if( challengeIntroMessages.size > 0) challengeIntroMessages.size else 1

    (challengeIntroMessages lift (rnd.nextInt(count)))
      .get
      .format(
        challenge.name.getOrElse(""),
        questionCount
      )
  }

  def getStreakingChallengeIntroMessage(challenge: Challenge, questionCount: Int ): String = {

    val count = if(challengeStreakingIntroMessages.size > 0) challengeStreakingIntroMessages.size else 1

    (challengeStreakingIntroMessages lift (rnd.nextInt(count)))
      .get
      .format(
        challenge.name.getOrElse(""),
        questionCount
      )
  }

  def getDictionaryNotFoundMessage(): String = {

    val count = if( dictionaryNotFoundMessages.size > 0) dictionaryNotFoundMessages.size else 1

    (dictionaryNotFoundMessages lift (rnd.nextInt(count))).getOrElse("Sorry! I don't know this.")
  }

  def getDictionaryInvalidPosMessage(): String = {

    val count = if( dictionaryInvalidPosMessages.size > 0) dictionaryInvalidPosMessages.size else 1

    (dictionaryInvalidPosMessages lift (rnd.nextInt(count))).getOrElse("Sorry! I don't know this.")
  }

  def getSuggestLearnMsg(): String = {

    val count = if( suggestLearnMessages.size > 0) suggestLearnMessages.size else 1

    (suggestLearnMessages lift (rnd.nextInt(count))).getOrElse("Hi there! Let's learn something new.")
  }

  def getUnrecognizedMsg(): String = {

    val count = if( unrecognizedMessages.size > 0) unrecognizedMessages.size else 1

    (unrecognizedMessages lift (rnd.nextInt(count))).getOrElse("Sorry! I didn't get that.")
  }

  def getAfterReviewSuggestedMsg(): String = {
    val count = if( afterReviewSuggestedMessages.size > 0) afterReviewSuggestedMessages.size else 1
    (afterReviewSuggestedMessages lift (rnd.nextInt(count)))
      .getOrElse("Remember to improve and broaden your knowledge every day!")
  }


  def getReviewInfoCardMsg(): String =  review.getReviewInfoCardMsg()

  def getHasDueCardMsg(count: Int) = review.getHasDueCardMsg(count)

  def getHasDueCardSuggestedMsg(count: Long) = review.getHasDueCardSuggestedMsg(count)

  def getNoDueCardMsg(): String =  review.getNoDueCardMsg()

  def getReviewCorrectMsg(): String =  review.getCorrectMsg()

  def getReviewIncorrectMsg(): String = review.getIncorrectMsg()

  def getChallengeCorrectMsg(examinationData: ExaminationData): String =  challenge.getCorrectMsg(examinationData)

  def getChallengeIncorrectMsg(): String = challenge.getIncorrectMsg()

  def getReviewResultMsg(passedPercent: Double, correct: Int, total: Int): String =  review.getReviewResultMsg(passedPercent, correct,total)

  def getChallengeResultMsg(name: String,
                            correctQuestionCount: Int,
                            totalQuestionCount: Int,
                            myRank: LeaderBoardItem): String =  {
    challenge.getChallengeResultMsg(name, correctQuestionCount, totalQuestionCount, myRank)
  }

  def getChallengeChampionResultMsg(name: String,
                            correctQuestionCount: Int,
                            totalQuestionCount: Int,
                            myRank: LeaderBoardItem): String =  {
    challenge.getChampionResultMsg(name, correctQuestionCount, totalQuestionCount, myRank)
  }


  def getCourseIntroducedMessage(name: String, desc: String): String = {

    val count = if (courseIntroducedMessages.size > 0) courseIntroducedMessages.size else 1

    (courseIntroducedMessages lift (rnd.nextInt(count)))
      .get
      .format(name, desc)
  }

  def getSectionIntroducedMessage(name: String, desc: String): String = {

    val count = if (sectionIntroducedMessages.size > 0) sectionIntroducedMessages.size else 1

    (sectionIntroducedMessages lift (rnd.nextInt(count)))
      .get
      .format(name, desc)
  }

  def getTopicIntroducedMessage(deckName: String, desc: String): String = {

    val count = if (topicIntroducedMessages.size > 0) topicIntroducedMessages.size else 1

    (topicIntroducedMessages lift (rnd.nextInt(count)))
      .get
      .format(deckName, desc)
  }

  def getCourseCompletedMessage(courseName: String): String = {

    val count = if (courseCompletedMessages.size > 0) courseCompletedMessages.size else 1

    (courseCompletedMessages lift (rnd.nextInt(count)))
      .getOrElse("Great! You have just completed the course: %s")
      .format(courseName)
  }



  def getTopicCompletedMessage(deckName: String): String = {

    val count = if (topicCompletedMessages.size > 0) topicCompletedMessages.size else 1

    (topicCompletedMessages lift (rnd.nextInt(count)))
      .get
      .format(deckName)
  }


  def getInvalidInputFibMsg(): String =  review.getInvalidInputFibMsg()

  def getInvalidInputMCMsg(): String =  review.getInvalidInputMCMsg()

  def getActionConf(name: String) = action.get(name).get

  def getReviewAction() = getActionConf("review")
  def getLearnAction() = getActionConf("learn")
  def getLearnYesAction() = getActionConf("learn_word_yes")
  def getLearnNoAction() = getActionConf("learn_word_no")
  def getLearnContinueAction() = getActionConf("learn_word_continue")
  def getResetAction() = getActionConf("reset")
  def getPrevPageAction() = getActionConf("see_less")
  def getNextPageAction() = getActionConf("see_more")
  def getSkipAction() = getActionConf("skip")
  def getKnownAction() = getActionConf("known_question")
  def getDontKnownAction() = getActionConf("dont_known_question")
  def getExitAction() = getActionConf("exit")
  def getCompleteChallengeAction() = getActionConf("challenge_submit")


  def getHelpMsg(): String = {
    val count = if (helpMessages.size > 0) helpMessages.size else 1
    (helpMessages lift (rnd.nextInt(count)))
      .getOrElse("")
  }

  def getNoCourseMsg(): String = {

    val count = if (noCourseMessages.size > 0) noCourseMessages.size else 1

    (noCourseMessages lift (rnd.nextInt(count))).getOrElse("No course was found.")
  }

  def getLearnWordCompletedAndDudeDateMsg(name: String, dudeDateStr: String): String = {

    val count = if (learnWordCompletedExplainMessages.size > 0) learnWordCompletedExplainMessages.size else 1

    (learnWordCompletedExplainMessages lift (rnd.nextInt(count)))
      .get
      .format(name, dudeDateStr)
  }


  def getChallengeListingMsg(): String = {

    val count = if (challengeListingMessages.size > 0) challengeListingMessages.size else 1

    (challengeListingMessages lift (rnd.nextInt(count))).get
  }

  def getCourseListingMsg(): String = {

    val count = if (courseListingMessages.size > 0) courseListingMessages.size else 1

    (courseListingMessages lift (rnd.nextInt(count))).getOrElse("Great! Here are some courses for you:")
  }

  def getOutOfIndexMsg(): String = {

    val count = if (outOfIndexMessages.size > 0) outOfIndexMessages.size else 1

    (outOfIndexMessages lift (rnd.nextInt(count))).getOrElse("Out of the order! Please try again.")
  }

  def getAskToLearnWordMsg(word: String): String = {

    val count = if (askToLearnWordMessages.size > 0) askToLearnWordMessages.size else 1

    (askToLearnWordMessages lift (rnd.nextInt(count)))
      .getOrElse("Do you want to learn \"%s\"?")
      .format(word)
  }

  def getNoWordToLearnMessages(): String = {

    val count = if (noWordToLearnMessages.size > 0) noWordToLearnMessages.size else 1

    (noWordToLearnMessages lift (rnd.nextInt(count))).getOrElse("No card to learn this word.")
  }

}



case class ReviewBotConfig(reviewInfoComponentMessages: Seq[String],
                           hasDueCardMessages: Seq[String],
                           hasDueCardSuggestedMessages: Seq[String],
                           noDueCardMessages: Seq[String],
                           fibInvalidInputMessages: Seq[String],
                           mcInvalidInputMessages: Seq[String],
                           correctAnswerMessages: Seq[String],
                           incorrectAnswerMessages: Seq[String],
                           showResultMessages: Seq[String]) {


  def getReviewInfoCardMsg(): String = {

    val count = if( reviewInfoComponentMessages.size > 0) reviewInfoComponentMessages.size else 1

    (reviewInfoComponentMessages lift (rnd.nextInt(count))).getOrElse("Do you know this?")
  }

  def getHasDueCardMsg(totalCard: Int): String = {

    val count = if( hasDueCardMessages.size > 0) hasDueCardMessages.size else 1

    (hasDueCardMessages lift (rnd.nextInt(count)))
      .getOrElse("You have %d cards to review today.")
      .format(totalCard)
  }

  def getHasDueCardSuggestedMsg(totalCard: Long): String = {

    val count = if( hasDueCardSuggestedMessages.size > 0) hasDueCardSuggestedMessages.size else 1

    (hasDueCardSuggestedMessages lift (rnd.nextInt(count)))
      .getOrElse("You have %d cards to review.\nType: \"REVIEW\" or click the below button to review now.")
      .format(totalCard)
  }

  def getNoDueCardMsg(): String = {

    val count = if( noDueCardMessages.size > 0) noDueCardMessages.size else 1

    (noDueCardMessages lift (rnd.nextInt(count))).getOrElse("You have no cards to review today.")
  }

  def getCorrectMsg(): String = {

    val count = if( correctAnswerMessages.size > 0) correctAnswerMessages.size else 1

    (correctAnswerMessages lift (rnd.nextInt(count))).getOrElse("Great answer!")
  }

  def getIncorrectMsg(): String = {

    val count = if( incorrectAnswerMessages.size > 0) incorrectAnswerMessages.size else 1

    (incorrectAnswerMessages lift (rnd.nextInt(count))).getOrElse("Sorry, incorrect answer!")
  }

  def getReviewResultMsg(passedPercent: Double, correct: Int, total: Int): String = {

    val count = if (showResultMessages.size > 0) showResultMessages.size else 1

    (showResultMessages lift (rnd.nextInt(count)))
      .getOrElse("Congratulation! You've completed the REVIEW with %02.1f passed.\nWhere, The number of correct answers is: %d/%d.")
      .format(passedPercent,correct, total)

  }

  def getInvalidInputFibMsg(): String = {

    val count = if( fibInvalidInputMessages.size > 0) fibInvalidInputMessages.size else 1

    (fibInvalidInputMessages lift (rnd.nextInt(count))).getOrElse("Invalid answer input!")
  }

  def getInvalidInputMCMsg(): String = {

    val count = if( mcInvalidInputMessages.size > 0) mcInvalidInputMessages.size else 1

    (mcInvalidInputMessages lift (rnd.nextInt(count))).getOrElse("Invalid answer input!")
  }
}


case class ChallengeBotConfig(correctAnswerMessages: Seq[String],
                              incorrectAnswerMessages: Seq[String],
                              resultMessages: Seq[String],
                              championResultMessages: Seq[String]
                             ) {

  def getCorrectMsg(examinationData: ExaminationData): String = {

    val count = if( correctAnswerMessages.size > 0) correctAnswerMessages.size else 1

    (correctAnswerMessages lift (rnd.nextInt(count)))
      .get
      .format(
        examinationData.getTotalAnsweredQuestions(),
        examinationData.totalCount,
        examinationData.getPoints()
      )
  }

  def getIncorrectMsg(): String = {

    val count = if( incorrectAnswerMessages.size > 0) incorrectAnswerMessages.size else 1

    (incorrectAnswerMessages lift (rnd.nextInt(count))).getOrElse("Sorry, incorrect answer!")
  }


  def getChallengeResultMsg(name: String,
                            correctQuestionCount: Int,
                            totalQuestionCount: Int,
                            myRank: LeaderBoardItem): String = {

    val count = if (resultMessages.size > 0) resultMessages.size else 1

    (resultMessages lift (rnd.nextInt(count)))
      .get.format(
      name,
      myRank.point,
      myRank.rank
    )

  }

  def getChampionResultMsg(name: String,
                           correctQuestionCount: Int,
                           totalQuestionCount: Int,
                           myRank: LeaderBoardItem): String = {

    val count = if (championResultMessages.size > 0) championResultMessages.size else 1

    (championResultMessages lift (rnd.nextInt(count))).get.format(
        name,
        myRank.point
    )

  }
}