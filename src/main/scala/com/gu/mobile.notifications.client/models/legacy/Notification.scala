package com.gu.mobile.notifications.client.models.legacy

import java.time.Duration
import java.util.UUID
import com.gu.mobile.notifications.client.models.Importance.Importance
import com.gu.mobile.notifications.client.models.Topic
import play.api.libs.json._
import com.gu.mobile.notifications.client.models.Editions._


case class Target(
  regions: Set[Edition],
  topics: Set[Topic]
)

object Target {
  implicit val jf = Json.writes[Target]
}

case class Recipient(userId: String)

object Recipient {
  implicit val jf = Json.writes[Recipient]
}
case class Notification(
  `type`: NotificationType,
  uniqueIdentifier: String = UUID.randomUUID.toString,
  sender: String,
  target: Target,
  timeToLiveInSeconds: Int = Notification.DefaultTimeToLiveSeconds,
  payloads: MessagePayloads,
  metadata: Map[String, String],
  importance: Importance
)

object Notification {
  val DefaultTimeToLiveSeconds = Duration.ofHours(48).getSeconds.toInt
  implicit val jf = Json.writes[Notification]
}