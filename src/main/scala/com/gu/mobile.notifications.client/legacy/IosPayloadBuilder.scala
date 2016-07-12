package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.IOSMessagePayload
import com.gu.mobile.notifications.client.models.legacy.{IosKeys => keys}
import com.gu.mobile.notifications.client.models.legacy.IosMessageTypes._

object IosPayloadBuilder extends PlatformPayloadBuilder{

  def build(payload: NotificationPayload) = payload match {
    case ga: GoalAlertPayload => buildGoalAlert(ga)
    case bn: BreakingNewsPayload => buildBreakingNews(bn)
    case cn: ContentAlertPayload => buildContentAlert(cn)
  }

  private def buildGoalAlert(payload: GoalAlertPayload) = {
    IOSMessagePayload(
      payload.message,
      Map(keys.MessageType -> keys.GoalAlertType,
        keys.Uri -> replaceHost(payload.mapiUrl),
        keys.UriType -> FootballMatch.toString)
    )
  }

  private def buildBreakingNews(payload: BreakingNewsPayload) = {
    IOSMessagePayload(
      body = payload.message,
      customProperties = iosProperties(payload),
      category = iosCategory(payload)
    )
  }

  private def buildContentAlert(payload: ContentAlertPayload) = {
    IOSMessagePayload(
      body = payload.title,
      customProperties = iosProperties(payload),
      category = iosCategory(payload)
    )
  }

  private def iosCategory(payload: NotificationWithLink) = payload.link match {
    case guardianLink: GuardianLinkDetails => guardianLink.shortUrl.map(_ => "ITEM_CATEGORY")
    case _ => None
  }

  private def iosProperties(payload: NotificationWithLink) = {

    val legacyIosLink = payload.link match {
      case GuardianLinkDetails(_, Some(url), _, _, _, _) => s"x-gu://" + new URI(url).getPath
        //TODO IS THIS CORRECT OR SHOULD IT BE x-gu:///contentApiId?
      case details: GuardianLinkDetails => details.webUrl
      case ExternalLink(url) => url
    }

    val link = toPlatformLink(payload.link)
    Map(
      keys.MessageType -> NewsAlert,
      keys.NotificationType -> payload.`type`.toString,
      keys.LegacyLink -> legacyIosLink,
      keys.Topics -> payload.topic.map(_.toTopicString).mkString(","),
      keys.Uri -> link.uri,
      keys.UriType -> link.`type`.toString
    )
  }
}
