package xed.chatbot

import java.util

import com.fasterxml.jackson.databind.JsonNode
import com.google.cloud.dialogflow.v2beta1.Intent.Message.Platform
import com.google.cloud.dialogflow.v2beta1.QueryResult
import com.google.protobuf.util.JsonFormat
import com.twitter.util.Future
import xed.api_service.service.NLPService
import xed.api_service.util.{Implicits, JsonUtils, ZConfig}
import xed.chatbot.domain.{ChatMessage, IntentActionInfo, IntentActionType, MessageType}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

trait ActionResolver {
  def resolve(chatMessage: ChatMessage)  : Future[Option[IntentActionInfo]]
}



case class SystemActionResolver() extends ActionResolver {

  val sysCommands = ZConfig.getMap("bot.sys_commands").map(entry=>{
    entry._1 -> entry._2.asInstanceOf[util.ArrayList[String]].toSeq
  })

  override def resolve(chatMessage: ChatMessage): Future[Option[IntentActionInfo]] = {
    chatMessage.messageType.getOrElse(MessageType.TEXT) match {
      case MessageType.REPLY_MC => Future.value(Some(IntentActionInfo(
        actionType = IntentActionType.UNKNOWN,
        confidence = Some(1.0f),
        isFallback = true,
        isRequiredParamCompleted = true,
        modifiedTime = Some(System.currentTimeMillis()),
        createdTime = Some(System.currentTimeMillis())
      )))
      case MessageType.REPLY_FIB => Future.value(Some(IntentActionInfo(
          actionType = IntentActionType.UNKNOWN,
          confidence = Some(1.0f),
          isFallback = true,
          isRequiredParamCompleted = true,
          modifiedTime = Some(System.currentTimeMillis()),
          createdTime = Some(System.currentTimeMillis())
        )))
      case MessageType.TEXT => resolveSysCommands(chatMessage)
      case _ => Future.None
    }
  }

  private def resolveSysCommands(chatMessage: ChatMessage) : Future[Option[IntentActionInfo]] = Future {
    val matchCommand = sysCommands.foldLeft(Option[String](null))((r, commandCfg) => r match {
      case Some(_) => r
      case _ =>
        val command = commandCfg._1
        val isMatch = commandCfg._2.contains(chatMessage.text.getOrElse("").trim.toLowerCase)
        if(isMatch) Some(command)
        else None
    })

    matchCommand.map(sysCommand =>{
      IntentActionInfo(
        actionType = sysCommand,
        confidence = Some(1.0f),
        isFallback = false,
        isRequiredParamCompleted = true,
        contexts = chatMessage.currentAction.map(_.contexts).getOrElse(ListBuffer.empty),
        modifiedTime = Some(System.currentTimeMillis()),
        createdTime = Some(System.currentTimeMillis())
      )
    })

  }
}


case class NLPActionResolver(nlpService: NLPService) extends ActionResolver {


  override def resolve(chatMessage: ChatMessage): Future[Option[IntentActionInfo]] = Implicits.async {

    val intentActionInfo = query(chatMessage)
    Some(intentActionInfo)
  }


  private def query(chatMessage: ChatMessage) = {

    val contexts = chatMessage.currentAction
      .map(_.contexts)
      .getOrElse(Nil)

    val queryResult = nlpService.detectIntentText(
      chatMessage.sender.get,
      contexts = contexts,
      text = chatMessage.text.getOrElse(""),
      languageCode = chatMessage.languageCode.getOrElse("en")
    )

    val isFallback = queryResult.map(_.getIntent.getIsFallback).getOrElse(false)
    val confidence = queryResult.map(_.getIntentDetectionConfidence)
    val isRequiredParamCompleted = queryResult.map(_.getAllRequiredParamsPresent).getOrElse(true)
    val fulfillmentText = queryResult.map(_.getFulfillmentText)
      .flatMap(x => if(x!= null && x.nonEmpty)  Some(x.trim) else None)

    val outputContexts = queryResult
      .map(_.getOutputContextsList.toSeq)
      .getOrElse(Nil)



    IntentActionInfo(
      actionType = queryResult.map(_.getIntent.getDisplayName).getOrElse(IntentActionType.UNKNOWN),
      fulfillmentText = fulfillmentText,
      contexts = ListBuffer(outputContexts:_*),
      confidence = confidence,
      params = parseParamAsJSONString(queryResult),
      payloads = parsePayloads(queryResult),
      isFallback = isFallback,
      isRequiredParamCompleted = isRequiredParamCompleted,
      modifiedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }

  def parseParamAsJSONString(queryResult: Option[QueryResult]): Option[String] = {
    queryResult.map(_.getParameters)
      .map(struct => JsonFormat.printer().print(struct))
  }

  def parsePayloads(queryResult: Option[QueryResult]): Option[Seq[String]] = {
    val payloads = queryResult
      .map(_.getFulfillmentMessagesList.toSeq)
      .getOrElse(Seq.empty)
      .filter(x => x.getPlatform == Platform.PLATFORM_UNSPECIFIED ||x.getPlatform == Platform.UNRECOGNIZED )
      .map(_.getPayload)
      .map(struct => JsonFormat.printer().print(struct))

    Some(payloads)
  }

}