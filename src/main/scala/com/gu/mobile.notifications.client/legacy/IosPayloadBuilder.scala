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
        keys.MapiLink -> replaceHost(payload.mapiUrl))
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

    val (iosLegacyLink, maybeMapiLink) = payload.link match {
      case GuardianLinkDetails(_, Some(url), _, _, _, _) =>
        val path = new URI(url).getPath
        (s"x-gu://$path", Some(s"x-gu://MAPI/items$path"))
      case GuardianLinkDetails(capiId, _, _, _, _, _) => (s"http://www.theguardian.com/$capiId", Some(s"x-gu://MAPI/items/$capiId"))
      case ExternalLink(url) => (url, None)
    }

    mapWithOptionalValues(
      keys.MessageType -> NewsAlert,
      keys.NotificationType -> payload.`type`.toString,
      keys.Link -> iosLegacyLink,
      keys.Topics -> payload.topic.map(_.toTopicString).mkString(",")
    )(keys.MapiLink -> maybeMapiLink)
  }
}
