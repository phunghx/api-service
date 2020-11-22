package xed.api_service.service.notification

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import slack.api.BlockingSlackApiClient
import slack.models.Attachment


case class SlackMessage(channelId: String,
                        text: String,
                        username: Option[String] = None,
                        asUser: Option[Boolean] = None,
                        parse: Option[String] = None,
                        linkNames: Option[String] = None,
                        attachments: Option[Seq[Attachment]] = None,
                        unfurlLinks: Option[Boolean] = None,
                        unfurlMedia: Option[Boolean] = None,
                        iconUrl: Option[String] = None,
                        iconEmoji: Option[String] = None,
                        replaceOriginal: Option[Boolean] = None,
                        deleteOriginal: Option[Boolean] = None,
                        threadTs: Option[String] = None)

case class SlackBotConfig(username: String, asUser: Boolean, avatarUrl: String)



case class SlackNotificationWorker(slackApiClient: BlockingSlackApiClient,
                                   actorSystem: ActorSystem,
                                   globalChannel: String,
                                   botConfig: SlackBotConfig) extends Actor with ActorLogging {

  override def receive: Receive = {
    case x =>
      log.info("Received invalid object: {}", if (x != null) x.getClass.getName else "null")
  }

  private def postSlackMsg(msg: SlackMessage): Unit = {
    slackApiClient.postChatMessage(
      msg.channelId,
      msg.text,
      msg.username,
      msg.asUser,
      msg.parse,
      msg.linkNames,
      msg.attachments,
      msg.unfurlLinks,
      msg.unfurlMedia,
      msg.iconUrl,
      msg.iconEmoji,
      msg.replaceOriginal,
      msg.deleteOriginal,
      msg.threadTs
    )(actorSystem)
  }
}



class SlackNotificationService(slackApiClient: BlockingSlackApiClient,
                               nWorkers: Int,
                               actorSystem: ActorSystem,
                               globalChannel: String,
                               globalTicketChannel: String,
                               botConfig: SlackBotConfig) extends Actor with ActorLogging {

  var router = {
    val routees = Vector.fill(nWorkers) {
      val r = context.actorOf(Props(
        SlackNotificationWorker(slackApiClient,
          actorSystem,
          globalChannel,
          botConfig)
      ))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(
        SlackNotificationWorker(slackApiClient,
          actorSystem,
          globalChannel,
          botConfig)
      ))
      context watch r
      router = router.addRoutee(r)

    case x => router.route(x, sender())

  }
}

