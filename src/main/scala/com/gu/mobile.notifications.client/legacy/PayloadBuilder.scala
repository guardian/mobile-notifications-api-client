package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models.legacy._
import AndroidMessageTypes._
import AndroidKeys._
import com.gu.mobile.notifications.client.constants.{Android, Ios, Platforms}
import com.gu.mobile.notifications.client.messagebuilder.InternationalEditionSupport
import com.gu.mobile.notifications.client.models.NotificationTypes.{BreakingNews, Content => ContentNotification}
import com.gu.mobile.notifications.client.models.Regions._
import com.gu.mobile.notifications.client.models._

import scala.PartialFunction._

object PayloadBuilder extends InternationalEditionSupport {

  def buildNotification(notif: NotificationPayload, sender: String, platforms: Set[Platforms]) = notif match {
    case bnp: BreakingNewsPayload => buildBreakingNewsAlert(bnp, sender, platforms)
    case cap: ContentAlertPayload => buildContentAlert(cap, sender, platforms)
    case gap: GoalAlertPayload => buildGoalAlert(gap, sender, platforms)
  }

  private def buildBreakingNewsAlert(bnp: BreakingNewsPayload, sender: String, platforms: Set[Platforms]) = Notification(
    `type` = BreakingNews,
    sender = sender,
    target = Target(editionsFrom(bnp) flatMap regions.get, Set.empty),
    payloads = breakingNewsAlertPayloads(bnp, platforms),
    metadata = Map(
      "title" -> bnp.title,
      "message" -> bnp.message,
      "link" -> bnp.link.toString
    )
  )

  private def buildContentAlert(cap: ContentAlertPayload, sender: String, platforms: Set[Platforms]) = Notification(
    `type` = ContentNotification,
    sender = sender,
    target = Target(Set.empty, Set.empty),
    payloads = contentAlertPayloads(cap, platforms),
    metadata = Map(
      "title" -> cap.title,
      "message" -> cap.message,
      "link" -> cap.link.toString
    )
  )

  private def buildGoalAlert(gap: GoalAlertPayload, sender: String, platforms: Set[Platforms]) = Notification(
    `type` = ContentNotification,
    sender = sender,
    target = Target(Set.empty, Set.empty),
    payloads = goalAlertPayload(gap, platforms),
    metadata = Map(
      "title" -> gap.title,
      "message" -> gap.message,
      "link" -> gap.mapiUrl
    )
  )

  private def breakingNewsAlertPayloads(message: BreakingNewsPayload, platforms: Set[Platforms]) = MessagePayloads(
    ios = if (platforms(Ios)) Some(buildIosPayload(message)) else None,
    android = if (platforms(Android)) Some(buildAndroidBreakingNewsPayloads(message)) else None
  )

  private def contentAlertPayloads(message: ContentAlertPayload, platforms: Set[Platforms]) = MessagePayloads(
    ios = if (platforms(Ios)) Some(buildIosPayload(message)) else None,
    //android = if (platforms(Android)) Some(buildAndroidContentAlertPayloads(message)) else None
    android = None
  )

  private def goalAlertPayload(message: GoalAlertPayload, platforms: Set[Platforms]) = MessagePayloads(
    //ios = if (platforms(Ios)) Some(buildIosGoalAlertPayload(message)) else None,
    //android = if (platforms(Android)) Some(buildAndroidGoalAlertPayload(message)) else None
    ios = None,
    android = None
  )


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

    val IOSMessageType = "t"

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
      IOSMessageType -> "m",
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