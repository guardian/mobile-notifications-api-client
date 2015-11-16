package com.gu.mobile.notifications.client.models

import java.net.URL

sealed case class GuardianItemType(mobileAggregatorPrefix: String)

object GITSection extends GuardianItemType("section")
object GITTag extends GuardianItemType("latest")
object GITContent extends GuardianItemType("item-trimmed")

sealed trait Link

case class ExternalLink(url: String) extends Link

case class GuardianLinkDetails(contentApiId: String, shortUrl: Option[String], title: String, thumbnail: Option[String], git: GuardianItemType) extends Link {
  val webUrl = s"http://www.theguardian.com/$contentApiId"
}

sealed trait GoalType
object OwnGoalType extends GoalType
object PenaltyGoalType extends GoalType
object DefaultGoalType extends GoalType

sealed trait PayloadType
object BreakingNewsPayloadType extends PayloadType
object ContentAlertPayloadType extends PayloadType
object GoalAlertPayloadType extends PayloadType

sealed trait NotificationPayload {
  def title: String
  def notificationType: String
  def message: String
  def thumbnailUrl: Option[URL]
  def debug: Boolean
}

sealed trait NotificationWithLink extends NotificationPayload {
  def link: Link
}

case class BreakingNewsPayload(
  title: String,
  notificationType: String = "news",
  message: String,
  thumbnailUrl: Option[URL],
  editions: Set[String],
  link: Link,
  imageUrl: Option[String],
  debug: Boolean
) extends NotificationWithLink

case class ContentAlertPayload(
  title: String,
  notificationType: String = "content",
  message: String,
  thumbnailUrl: Option[URL],
  link: Link,
  debug: Boolean,
  shortUrl: String
) extends NotificationWithLink

case class GoalAlertPayload(
  title: String,
  notificationType: String = "goal",
  message: String,
  thumbnailUrl: Option[URL] = None,
  goalType: GoalType,
  awayTeamName: String,
  awayTeamScore: Int,
  homeTeamName: String,
  homeTeamScore: Int,
  scoringTeamName: String,
  scorerName: String,
  goalMins: Int,
  otherTeamName: String,
  matchId: String,
  mapiUrl: String,
  debug: Boolean,
  addedTime: Option[String]
) extends NotificationPayload