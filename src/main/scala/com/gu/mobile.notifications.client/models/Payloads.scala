package com.gu.mobile.notifications.client.models

import java.net.{URI, URL}
import com.gu.mobile.notifications.client.models.Importance.Importance
import com.gu.mobile.notifications.client.models.legacy.Topic

sealed case class GuardianItemType(mobileAggregatorPrefix: String)

object GITSection extends GuardianItemType("section")
object GITTag extends GuardianItemType("latest")
object GITContent extends GuardianItemType("item-trimmed")

sealed trait Link {
  override def toString = this match {
    case gl: GuardianLinkDetails => gl.webUrl
    case ExternalLink(url) => url
  }
  def toShortUrl = this match {
    case GuardianLinkDetails(_, Some(url), _, _, _) => s"x-gu://" + new URI(url).getPath
    case GuardianLinkDetails(contentApiId, _, _, _, _) => s"x-gu://www.theguardian.com/$contentApiId"
    case ExternalLink(url) => url
  }
}

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
  def sender: String
  def importance: Importance
  def topic: Set[Topic]
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
  sender: String,
  editions: Set[String],
  link: Link,
  imageUrl: Option[String],
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean
) extends NotificationWithLink

case class ContentAlertPayload(
  title: String,
  notificationType: String = "content",
  message: String,
  thumbnailUrl: Option[URL],
  sender: String,
  link: Link,
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean,
  shortUrl: String
) extends NotificationWithLink

case class GoalAlertPayload(
  title: String,
  notificationType: String = "goal",
  message: String,
  thumbnailUrl: Option[URL] = None,
  sender: String,
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
  importance: Importance,
  topic: Set[Topic],
  debug: Boolean,
  addedTime: Option[String]
) extends NotificationPayload