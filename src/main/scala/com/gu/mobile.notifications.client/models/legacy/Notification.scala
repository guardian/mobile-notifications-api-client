package com.gu.mobile.notifications.client.models.legacy

import com.gu.mobile.notifications.client.models.NotificationTypes.NotificationType
import play.api.libs.json._
import com.gu.mobile.notifications.client.models.Regions._

object Topic {
  val FootballTeamType = "football-team"
  val FootballMatchType = "football-match"
  val UserType = "user-type"

  val NewsstandIos = Topic(`type` = "newsstand", `name` = "newsstandIos")

  implicit val jf = Json.format[Topic]
}

/** Generic topic for a push notification:
  *
  * Examples:
  *   - Topic("football-match", "1234")
  *   - Topic("content", "/environment/2013/oct/21/britain-nuclear-power-station-hinkley-edf")
  */
case class Topic(
  `type`: String,
  name: String
) {
  def toTopicString = `type` + "//" + name
}

case class Target(
  regions: Set[Region],
  topics: Set[Topic]
)

object Target {
  implicit val jf = Json.format[Target]
}

case class Recipient(userId: String)

object Recipient {
  implicit val jf = Json.format[Recipient]
}
case class Notification(
  /** Type has no meaning in Guardian Notifications API - it is for ease of querying and must be established by
    * convention between clients.
    *
    * e.g., if Breaking News wants to be able to keep track of what notifications it's sent, it should use a type by
    * which it can query the API ('news'). No other clients should use this.
    */
  `type`: NotificationType,
  /** Used for de-duplication */
  uniqueIdentifier: String,
  sender: String,
  target: Target,
  timeToLiveInSeconds: Int,
  payloads: MessagePayloads,
  /** Guardian Notifications API is agnostic as to what sort of notifications are sent (the idea being we can use this
    * service to send both news alerts and push notifications to wake up an iPad app in the morning to download today's
    * edition of the news). As such, the kind of data a client might want associated with a given notification for
    * display purposes is not dictated by the API. We allow the user to store a map of String -> String for this
    * purpose.
    *
    * If the client wants anything more advanced they should maintain their own data store using the notification ID
    * returned by Pushy when they deliver the notification.
    */
  metadata: Map[String, String]
)

object Notification {
  implicit val jf = Json.format[Notification]
}