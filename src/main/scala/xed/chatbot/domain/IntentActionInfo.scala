package xed.chatbot.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.google.cloud.dialogflow.v2beta1.Context
import xed.api_service.util.JsonUtils

import scala.collection.mutable.ListBuffer

object IntentActionType {

  val UNKNOWN = "unknown"
  val SYS_EXIT = "kiki.sys.exit"
  val HELP = "kiki.help"
  val LANGUAGE_LEVEL_CHANGED = "kiki.level.changed"
  val LEARN = "kiki.learn"
  val LEARN_INTRODUCTION = "kiki.learn.introduction"

  val LEARN_LEVEL_CHANGED = "kiki.learn.level_changed"
  val LEARN_TEST = "kiki.learn.test"
  val LEARN_TEST_KNOWN_QUESTION = "kiki.learn.test.known_question"
  val LEARN_TEST_DONT_KNOW_QUESTION = "kiki.learn.test.dont_known_question"
  val LEARN_TEST_EXIT = "kiki.learn.test.exit"

  val LEARN_YES = "kiki.learn.yes"
  val LEARN_NO = "kiki.learn.no"
  val LEARN_CONTINUE = "kiki.learn.continue"
  val LEARN_STOP = "kiki.learn.stop"

  val REVIEW = "kiki.review"
  val REVIEW_KNOWN_QUESTION = "kiki.review.known_question"
  val REVIEW_DONT_KNOW_QUESTION = "kiki.review.dont_known_question"
  val REVIEW_CONTINUE = "kiki.review.continue"
  val REVIEW_SKIP = "kiki.review.skip"
  val REVIEW_STOP = "kiki.review.exit"

  val CHALLENGE_CREATE = "challenge.create"
  val CHALLENGE_JOIN = "challenge.join"
  val CHALLENGE_PLAY = "challenge.play"
  val CHALLENGE_KNOWN_QUESTION = "challenge.play.known_question"
  val CHALLENGE_DONT_KNOWN_QUESTION = "challenge.play.dont_known_question"
  val CHALLENGE_CONTINUE = "challenge.play.continue"
  val CHALLENGE_SUBMIT = "challenge.play.submit"

  val DICT_SEARCH = "kiki.dictionary.search"
  val DICT_EXIT = "kiki.dictionary.exit"
}

@SerialVersionUID(20200602L)
case class IntentActionInfo(actionType: String,
                            fulfillmentText : Option[String] = None,
                            contexts:ListBuffer[Context] = ListBuffer.empty,
                            confidence: Option[Float] = Some(1.0f),
                            params: Option[String] = None,
                            payloads: Option[Seq[String]] = None,
                            isFallback:  Boolean = false,
                            isRequiredParamCompleted:  Boolean = true,
                            modifiedTime: Option[Long],
                            createdTime: Option[Long]) extends Serializable {



  @JsonIgnore
  def getOrCreateContext(ctx: String, fn :(String) => Context) =  {
    contexts
      .filter(_.getName.endsWith(s"/${ctx}"))
      .headOption.fold({
      val context = fn(ctx)
      contexts.append(context)
      context
    })(x => x)
  }

  def findContext(ctx: String): Option[Int] =  {
    contexts.zipWithIndex
      .filter(_._1.getName.endsWith(s"/${ctx}"))
      .map(_._2)
      .headOption
  }

  def removeContext(ctx: String) : Unit = {
    findContext(ctx) match {
      case Some(index) =>
        val x = contexts(index)
        contexts.remove(index)
        contexts.append(Context.newBuilder(x)
          .setLifespanCount(0)
          .build())
      case _ =>
    }
  }

  def updateContextData(ctx: String, param: ContextData, fn :(String) => Context) : Unit = {
    var context = findContext(ctx) match {
      case Some(index) =>
        val x = contexts(index)
        contexts.remove(index)
        x
      case _ =>
        fn(ctx)
    }
    context = Context.newBuilder(context)
      .setParameters(param.asStruct())
      .setLifespanCount(5)
      .build()
    contexts.append(context)
  }

  def getContextData[T: Manifest](ctx: String, fn :(String) => Context) = {
    val struct = getOrCreateContext(ctx, fn).getParameters
    JsonUtils.fromStruct[T](struct)
  }

  @JsonIgnore
  def getCourseIdParam(): Option[String] = {
    params.map(JsonUtils.fromJson[JsonNode](_))
      .flatMap(x => Option(x.at("/course_id").asText(null)))
  }

  def parseSuggestionsFromPayload(): Seq[UserAction] = {
    payloads
      .map(_.map(JsonUtils.fromJson[JsonNode](_)))
      .getOrElse(Seq.empty)
      .map(_.at("/suggestions"))
      .filter(_.isArray)
      .map(x => JsonUtils.fromJson[Seq[UserAction]](x.toString))
      .flatten
  }


  def parseActionsFromPayload(): Seq[UserAction] = {
    payloads
      .map(_.map(JsonUtils.fromJson[JsonNode](_)))
      .getOrElse(Seq.empty)
      .map(_.at("/actions"))
      .filter(_.isArray)
      .map(x => JsonUtils.fromJson[Seq[UserAction]](x.toString))
      .flatten
  }
}