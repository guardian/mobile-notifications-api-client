package com.gu.mobile.notifications.client.legacy

import java.util.UUID
import java.util.concurrent.TimeUnit.MINUTES

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._

import scala.concurrent.duration.Duration

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
      payloads = buildPlatFormPayloads(strippedPayload, editions),
      metadata = Map(
        "title" -> bnp.title,
        "message" -> bnp.message,
        "link" -> bnp.link.toString
      ),
      importance = bnp.importance
    )
  }

  private def buildContentAlert(cap: ContentAlertPayload) = Notification(
    uniqueIdentifier = cap.derivedId,
    `type` = NotificationType.Content,
    sender = cap.sender,
    target = Target(Set.empty, cap.topic),
    payloads = buildPlatFormPayloads(cap),
    metadata = Map(
      "title" -> cap.title,
      "message" -> cap.message,
      "link" -> cap.link.toString
    ),
    importance = cap.importance
  )

  private def buildGoalAlert(gap: GoalAlertPayload) = {
    Notification(
      uniqueIdentifier = gap.derivedId,
      `type` = NotificationType.GoalAlert,
      sender = gap.sender,
      target = Target(Set.empty, gap.topic),
      payloads = buildPlatFormPayloads(gap),
      timeToLiveInSeconds = Duration(FootballDurations.MaxTotalMinutes - gap.goalMins, MINUTES).toSeconds.toInt,
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
  }

  private def buildPlatFormPayloads(notification: NotificationPayload, editions: Set[Edition] = Set.empty) = MessagePayloads(
    ios = Some(IosPayloadBuilder.build(notification)),
    android = Some(AndroidPayloadBuilder.build(notification, editions))
  )

}

private object FootballDurations {
  val RegularTimeMinutes = 90
  val ExtraTimeMinutes = 30
  val MiscMinutes = 30
  val MaxTotalMinutes = RegularTimeMinutes + ExtraTimeMinutes + MiscMinutes
}