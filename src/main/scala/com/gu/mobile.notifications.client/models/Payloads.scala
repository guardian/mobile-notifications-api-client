package com.gu.mobile.notifications.client.models

package models

sealed trait GoalType
object OwnGoalType extends GoalType
object PenaltyGoalType extends GoalType
object DefaultGoalType extends GoalType

sealed trait PayloadType
object BreakingNewsPayloadType extends PayloadType
object ContentAlertPayloadType extends PayloadType
object GoalAlertPayloadType extends PayloadType

trait NotificationPayload {
  def notificationType: String
  def ticker: String
  def message: String
  def title: String
  def thumbnailUrl: Option[String]
  def debug: Boolean
}

trait NotificationWithLink extends NotificationPayload {
  def link: String
}

sealed trait Link2
case class LinkFoo(a: String) extends Link2

case class BreakingNewsPayload2(
  notificationType: String = "news",
  ticker: String,
  title: String,
  message: String,
  debug: Boolean,
  editions: Set[String],
  link: Link2,
  thumbnailUrl: Option[String],
  imageUrl: Option[String]
) extends NotificationWithLink

case class ContentAlertPayload(
  notificationType: String = "content",
  ticker: String,
  message: String,
  title: String,
  thumbnailUrl: Option[String],
  link: String,
  debug: Boolean,

  shortUrl: String
) extends NotificationWithLink

case class GoalAlertPayload(
  notificationType: String = "goal",
  message: String,
  title: String,
  ticker: String,
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
  addedTime: Option[String],
  goalType: GoalType,
  thumbnailUrl: Option[String] = None
) extends NotificationPayload