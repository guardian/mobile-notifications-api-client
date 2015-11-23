package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models.legacy._
import AndroidMessageTypes._
import AndroidKeys._
import IosMessageTypes._
import IosKeys._
import com.gu.mobile.notifications.client.messagebuilder.InternationalEditionSupport
import com.gu.mobile.notifications.client.models.NotificationTypes.{BreakingNews, Content => ContentNotification}
import com.gu.mobile.notifications.client.models.Regions._
import com.gu.mobile.notifications.client.models._

import scala.PartialFunction._
//TODO MAYBE IT SHOULD BE CALLED SOMETHING LIKE NotificationBuilder or PayloadConverter or something like that because it builds notifications from payloads not the other way around
trait PayloadBuilder {
  def buildNotification(notification: NotificationPayload): Notification
}

object PayloadBuilderImpl extends PayloadBuilder with InternationalEditionSupport {

  def buildNotification(notification: NotificationPayload) = notification match {
    case bnp: BreakingNewsPayload => buildBreakingNewsAlert(bnp)
    case cap: ContentAlertPayload => throw new UnsupportedOperationException("Method not implemented")
    case gap: GoalAlertPayload => throw new UnsupportedOperationException("Method not implemented")
  }

  private def buildBreakingNewsAlert(bnp: BreakingNewsPayload) = Notification(
    `type` = BreakingNews,
    sender = bnp.sender,
    target = Target(editionsFrom(bnp) flatMap regions.get, bnp.topic),
    payloads = breakingNewsAlertPayloads(bnp),
    metadata = Map(
      "title" -> bnp.title,
      "message" -> bnp.message,
      "link" -> bnp.link.toString
    )
  )

  private def buildContentAlert(cap: ContentAlertPayload) = Notification(
    `type` = ContentNotification,
    sender = cap.sender,
    target = Target(Set.empty, cap.topic),
    payloads = contentAlertPayloads(cap),
    metadata = Map(
      "title" -> cap.title,
      "message" -> cap.message,
      "link" -> cap.link.toString
    )
  )

  private def buildGoalAlert(gap: GoalAlertPayload) = Notification(
    `type` = ContentNotification,
    sender = gap.sender,
    target = Target(Set.empty, gap.topic),
    payloads = goalAlertPayload(gap),
    metadata = Map(
      "title" -> gap.title,
      "message" -> gap.message,
      "link" -> gap.mapiUrl
    )
  )

  private def breakingNewsAlertPayloads(message: BreakingNewsPayload) = MessagePayloads(
    ios = Some(buildIosPayload(message)),
    android = Some(buildAndroidBreakingNewsPayloads(message))
  )

  private def contentAlertPayloads(message: ContentAlertPayload) = MessagePayloads(
    ios = Some(buildIosPayload(message)),
    android = Some(buildAndroidContentAlertPayloads(message))
  )

  private def goalAlertPayload(message: GoalAlertPayload) = MessagePayloads(
    ios = Some(buildIosGoalAlertPayload(message)),
    android = Some(buildAndroidGoalAlertPayload(message))
  )

  private def buildAndroidContentAlertPayloads(payload: ContentAlertPayload) = ???

  private def buildAndroidGoalAlertPayload(payload: GoalAlertPayload) = ???

  private def buildIosGoalAlertPayload(payload: GoalAlertPayload) = ???

  private def buildAndroidBreakingNewsPayloads(payload: BreakingNewsPayload) = {
    val androidLink = payload.link match {
      case GuardianLinkDetails(contentApiId, _, _, _, _) => s"x-gu://www.guardian.co.uk/$contentApiId"
      case ExternalLink(url) => url
    }

    val sectionLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITSection) => contentApiId
    }

    val tagLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITTag) => contentApiId
    }

    val edition = if (payload.editions.size == 1) Some(payload.editions.head) else None

    AndroidMessagePayload(
      Map(
        Type -> Custom,
        NotificationType -> payload.notificationType,
        Title -> payload.title,
        Ticker -> payload.message,
        Message -> payload.message,
        Debug -> payload.debug.toString,
        Editions -> payload.editions.mkString(","),
        Link -> androidLink
      ) ++ Seq(
        Section -> sectionLink,
        Edition -> edition,
        Keyword -> tagLink,
        ImageUrl -> payload.imageUrl,
        ThumbnailUrl -> payload.thumbnailUrl.map(_.toString)
      ).collect({
        case (k, Some(v)) => k -> v
      })
    )
  }

  private def buildIosPayload(payload: NotificationWithLink) = {

    val iosLink = payload.link match {
      case GuardianLinkDetails(_, Some(url), _, _, _) => s"x-gu://" + new URI(url).getPath
      case details: GuardianLinkDetails => details.webUrl
      case ExternalLink(url) => url
    }

    val iosCategory = payload.link match {
      case guardianLink: GuardianLinkDetails => guardianLink.shortUrl.map(_ => "ITEM_CATEGORY")
      case _ => None
    }

    val properties = Map(
      IOSMessageType -> M,
      NotificationType -> BreakingNews.toString(),
      Link -> iosLink
    )

    IOSMessagePayload(
      body = payload.message,
      customProperties = properties,
      category = iosCategory
    )
  }

}