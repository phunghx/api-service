package xed.chatbot

import com.twitter.inject.Logging
import com.twitter.util.Future
import xed.chatbot.domain.{ChatMessage, IntentActionInfo, IntentActionType}
import xed.profiler.Profiler

object ActionResolveEngine {

  def buildFallbackActionInfo(chatMessage: ChatMessage): IntentActionInfo =   Profiler(s"ActionResolveEngine.buildFallbackActionInfo") {
    IntentActionInfo(
      actionType = IntentActionType.UNKNOWN,
      confidence = Some(0.0f),
      isFallback = true,
      isRequiredParamCompleted = true,
      modifiedTime = Some(System.currentTimeMillis()),
      createdTime = Some(System.currentTimeMillis())
    )
  }
}

abstract class ActionResolveEngine extends  Logging {
  protected lazy val clazz = getClass.getSimpleName
  protected val resolvers: Seq[ActionResolver]

  def resolve(chatMessage: ChatMessage) : Future[IntentActionInfo]
}


case class ActionResolveEngineImpl(resolvers: Seq[ActionResolver]) extends ActionResolveEngine {

  override def resolve(chatMessage: ChatMessage): Future[IntentActionInfo] =  Profiler(s"$clazz.resolve") {
    val fn = resolvers.foldLeft[Future[Option[IntentActionInfo]]](Future.None)((fn, resolver) => {
      fn.flatMap({
        case Some(x) => Future.value(Some(x))
        case _ => resolver.resolve(chatMessage)
      })
    })

    fn.map({
      case Some(r) =>
        info(s"Resolve ${chatMessage.messageType.getOrElse(domain.MessageType.TEXT)}: `${chatMessage.text.getOrElse("")}` as ${r.actionType} confidence: ${r.confidence.getOrElse(0.0f)}")
        r
      case _ => ActionResolveEngine.buildFallbackActionInfo(chatMessage)
    })
  }


}


