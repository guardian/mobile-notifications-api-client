package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models.NotificationTypes.{BreakingNews, Content => ContentNotification}
import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models.legacy.AndroidKeys.{Edition => EditionKey, Editions => EditionsKey, _}
import com.gu.mobile.notifications.client.models.legacy.AndroidMessageTypes._
import com.gu.mobile.notifications.client.models.legacy.IosKeys._
import com.gu.mobile.notifications.client.models.legacy.IosMessageTypes._
import com.gu.mobile.notifications.client.models.legacy._
import com.gu.mobile.notifications.client.models.{Link => _, _}

import scala.PartialFunction._

trait NotificationBuilder {
  def buildNotification(notification: NotificationPayload): Notification
}

object NotificationBuilderImpl extends NotificationBuilder {

  def buildNotification(notification: NotificationPayload) = notification match {
    case bnp: BreakingNewsPayload =>   buildBreakingNewsAlert(bnp)
    case cap: ContentAlertPayload => throw new UnsupportedOperationException("Method not implemented")
    case gap: GoalAlertPayload => throw new UnsupportedOperationException("Method not implemented")
  }
  private def extractEditions(bnp: BreakingNewsPayload) = {

  }
  private def buildBreakingNewsAlert(bnp: BreakingNewsPayload) = {

    val editions = bnp.topic.map(Edition.fromTopic).flatten
    val nonEditionTopics = bnp.topic.filter(Edition.fromTopic(_).isEmpty)
    val strippedPayload = bnp.copy(topic = nonEditionTopics)

    Notification(
      uniqueIdentifier = bnp.id,
      `type` = BreakingNews,
      sender = bnp.sender,
      target = Target(editions, nonEditionTopics),
      payloads = breakingNewsAlertPayloads(strippedPayload, editions),
      metadata = Map(
        "title" -> bnp.title,
        "message" -> bnp.message,
        "link" -> bnp.link.toString
      )
    )
  }

  private def buildContentAlert(cap: ContentAlertPayload) = Notification(
    uniqueIdentifier = cap.id,
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
    uniqueIdentifier = gap.id,
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

  private def breakingNewsAlertPayloads(message: BreakingNewsPayload, editions: Set[Edition]) = MessagePayloads(
    ios = Some(buildIosPayload(message)),
    android = Some(buildAndroidBreakingNewsPayload(message, editions))
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

  private def buildAndroidBreakingNewsPayload(payload: BreakingNewsPayload, editions: Set[Edition]) = {

    val sectionLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITSection) => contentApiId
    }

    val tagLink = condOpt(payload.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITTag) => contentApiId
    }

    AndroidMessagePayload(
      Map(
        Type -> Custom,
        NotificationType -> payload.`type`.toString,
        Title -> payload.title,
        Ticker -> payload.message,
        Message -> payload.message,
        Debug -> payload.debug.toString,
        EditionsKey -> editions.mkString(","),
        Link -> payload.link.toDeepLink,
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

  private def buildIosPayload(payload: NotificationWithLink) = {

    val iosCategory = payload.link match {
      case guardianLink: GuardianLinkDetails => guardianLink.shortUrl.map(_ => "ITEM_CATEGORY")
      case _ => None
    }

    val properties = Map(
      IOSMessageType -> NewsAlert,
      NotificationType -> BreakingNews.toString(),
      Link -> payload.link.toDeepLink,
      Topics -> payload.topic.map(_.toTopicString).mkString(",")
    )

    IOSMessagePayload(
      body = payload.message,
      customProperties = properties,
      category = iosCategory
    )
  }

}