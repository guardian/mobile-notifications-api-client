package com.gu.mobile.notifications.client.legacy

import java.util.UUID

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._

trait NotificationBuilder {
  def buildNotification(notification: NotificationPayload): Notification
}

object NotificationBuilderImpl extends NotificationBuilder {

  def buildNotification(notification: NotificationPayload) = notification match {
    case bnp: BreakingNewsPayload => buildBreakingNewsAlert(bnp)
    case cap: ContentAlertPayload => buildContentAlert(cap)
    case gap: GoalAlertPayload => buildGoalAlert(gap)
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
      payloads = payloads(strippedPayload, editions),
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
    payloads = payloads(cap),
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
    uniqueIdentifier = s"goalAlert/${gap.matchId}/${gap.homeTeamScore}-${gap.awayTeamScore}/${gap.goalMins}",
    `type` = NotificationType.GoalAlert,
    sender = gap.sender,
    target = Target(Set.empty, gap.topic),
    payloads = payloads(gap),
    timeToLiveInSeconds = (150 - gap.goalMins) * 60,
    metadata = Map(
      "matchId" -> gap.matchId,
      "homeTeamName" -> gap.homeTeamName,
      "homeTeamScore" -> gap.homeTeamScore.toString,
      "awayTeamName" -> gap.awayTeamName,
      "awayTeamScore" -> gap.awayTeamScore.toString,
      "scorer" -> gap.scorerName,
      "minute" -> gap.goalMins.toString
    ),
    importance = gap.importance
  )

  private def payloads(payload: NotificationPayload, editions: Set[Edition] = Set.empty) = MessagePayloads(
    ios = Some(IosPayloadBuilder.build(payload)),
    android = Some(AndroidPayloadBuilder.build(payload, editions))
  )


}