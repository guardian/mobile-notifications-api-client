package com.gu.mobile.notifications.client.legacy

import java.net.URI
import java.util.UUID

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.AndroidKeys.{NotificationType => NotificationTypeKey, Edition => EditionKey, Editions => EditionsKey, Link => LinkKey, _}
import com.gu.mobile.notifications.client.models.legacy.AndroidMessageTypes._
import com.gu.mobile.notifications.client.models.legacy.IosKeys._
import com.gu.mobile.notifications.client.models.legacy.IosMessageTypes._
import com.gu.mobile.notifications.client.models.legacy._

import scala.PartialFunction._

trait NotificationBuilder {
  def buildNotification(notification: NotificationPayload): Notification
}

object NotificationBuilderImpl extends NotificationBuilder {

  def buildNotification(notification: NotificationPayload) = notification match {
    case bnp: BreakingNewsPayload => buildBreakingNewsAlert(bnp)
    case cap: ContentAlertPayload => buildContentAlert(cap)
    case gap: GoalAlertPayload => throw new UnsupportedOperationException("Method not implemented")
  }

  private def buildBreakingNewsAlert(bnp: BreakingNewsPayload) = {

    val editions = bnp.topic.flatMap(Edition.fromTopic)
    val nonEditionTopics = bnp.topic.filter(Edition.fromTopic(_).isEmpty)
    val strippedPayload = bnp.copy(topic = nonEditionTopics)

    Notification(
      uniqueIdentifier = bnp.id,
      `type` = NotificationType.BreakingNews,
      sender = bnp.sender,
      target = Target(editions, nonEditionTopics),
      payloads = breakingNewsAlertPayloads(strippedPayload, editions),
      metadata = Map(
        "title" -> bnp.title,
        "message" -> bnp.message,
        "link" -> bnp.link.toString
      ),
      importance = bnp.importance
    )
  }

  private def buildContentAlert(cap: ContentAlertPayload) = Notification(
    uniqueIdentifier = contentAlertId(cap),
    `type` = NotificationType.Content,
    sender = cap.sender,
    target = Target(Set.empty, cap.topic),
    payloads = contentAlertPayloads(cap),
    metadata = Map(
      "title" -> cap.title,
      "message" -> cap.message,
      "link" -> cap.link.toString
    ),
    importance = cap.importance
  )

  private def contentAlertId(cap: ContentAlertPayload): String = {
    def newContentIdentifier(contentApiId: String): String = s"contentNotifications/newArticle/$contentApiId"
    def newBlockIdentifier(contentApiId: String, blockId: String): String = s"contentNotifications/newBlock/$contentApiId/$blockId"
    val contentCoordinates = cap.link match {
      case GuardianLinkDetails(contentApiId, _, _, _, _, blockId) => (Some(contentApiId), blockId)
      case _ => (None, None)
    }

    contentCoordinates match {
      case (Some(contentApiId), Some(blockId)) => newBlockIdentifier(contentApiId, blockId)
      case (Some(contentApiId), None) => newContentIdentifier(contentApiId)
      case (None, _) => UUID.randomUUID.toString
    }
  }


  private def buildGoalAlert(gap: GoalAlertPayload) = Notification(
    uniqueIdentifier = gap.id,
    `type` = NotificationType.GoalAlert,
    sender = gap.sender,
    target = Target(Set.empty, gap.topic),
    payloads = goalAlertPayload(gap),
    metadata = Map(
      "title" -> gap.title,
      "message" -> gap.message,
      "link" -> gap.mapiUrl.toString
    ),
    importance = gap.importance
  )

  private def breakingNewsAlertPayloads(payload: BreakingNewsPayload, editions: Set[Edition]) = MessagePayloads(
    ios = Some(buildIosPayload(payload)),
    android = Some(buildAndroidBreakingNewsPayload(payload, editions))
  )

  private def contentAlertPayloads(payload: ContentAlertPayload) = MessagePayloads(
    ios = Some(buildContentAlertIosPayload(payload)),
    android = Some(buildAndroidContentAlertPayloads(payload))
  )

  private def goalAlertPayload(payload: GoalAlertPayload) = MessagePayloads(
    ios = Some(buildIosGoalAlertPayload(payload)),
    android = Some(buildAndroidGoalAlertPayload(payload))
  )

  private def buildAndroidContentAlertPayloads(payload: ContentAlertPayload) = {
    AndroidMessagePayload(
      Map(
        Type -> Custom,
        Title -> payload.title,
        Ticker -> payload.message,
        Message -> payload.message,
        LinkKey ->  toAndroidLink(payload.link),
        Topics -> payload.topic.map(_.toTopicString).mkString(",")
      ) ++ payload.thumbnailUrl.map(ThumbnailUrl -> _.toString)
    )
  }

  private def buildAndroidGoalAlertPayload(payload: GoalAlertPayload) = ???

  private def buildIosGoalAlertPayload(payload: GoalAlertPayload) = ???

  private def toAndroidLink(link: Link) = link match {
    case GuardianLinkDetails(contentApiId, _, _, _, _, _) => s"x-gu://www.guardian.co.uk/$contentApiId"
    case ExternalLink(url) => url
  }

  private def buildAndroidBreakingNewsPayload(payload: BreakingNewsPayload, editions: Set[Edition]) = {

    val sectionLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITSection, _) => contentApiId
    }

    val tagLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITTag, _) => contentApiId
    }

    AndroidMessagePayload(
      Map(
        Type -> Custom,
        NotificationTypeKey -> payload.`type`.toString,
        Title -> payload.title,
        Ticker -> payload.message,
        Message -> payload.message,
        Debug -> payload.debug.toString,
        EditionsKey -> editions.mkString(","),
        LinkKey ->  toAndroidLink(payload.link),
        Topics -> payload.topic.map(_.toTopicString).mkString(",")
      ) ++ Seq(
        Section -> sectionLink,
        EditionKey -> (if (editions.size == 1) Some(editions.head.toString) else None),
        Keyword -> tagLink,
        ImageUrl -> payload.imageUrl,
        ThumbnailUrl -> payload.thumbnailUrl.map(_.toString)
      ).collect({
        case (k, Some(v)) => k -> v
      })
    )
  }



  private def iosCategory(payload: NotificationWithLink) = payload.link match {
    case guardianLink: GuardianLinkDetails => guardianLink.shortUrl.map(_ => "ITEM_CATEGORY")
    case _ => None
  }

  private def iosProperties(payload: NotificationWithLink) = {

    val iosLink = payload.link match {
      case GuardianLinkDetails(_, Some(url), _, _, _, _) => s"x-gu://" + new URI(url).getPath
      case details: GuardianLinkDetails => details.webUrl
      case ExternalLink(url) => url
    }

    Map(
      IOSMessageType -> NewsAlert,
      NotificationTypeKey -> payload.`type`.toString,
      LinkKey -> iosLink,
      Topics -> payload.topic.map(_.toTopicString).mkString(",")
    )
  }


  private def buildIosPayload(payload: NotificationWithLink) = {
    IOSMessagePayload(
      body = payload.message,
      customProperties = iosProperties(payload),
      category = iosCategory(payload)
    )
  }

  private def buildContentAlertIosPayload(payload: ContentAlertPayload) = {
    IOSMessagePayload(
      body = payload.title,
      customProperties = iosProperties(payload),
      category = iosCategory(payload)
    )
  }

}