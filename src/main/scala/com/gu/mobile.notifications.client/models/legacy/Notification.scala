package com.gu.mobile.notifications.client.models.legacy

import java.util.UUID
import com.gu.mobile.notifications.client.models.NotificationTypes.NotificationType
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
  /** Type has no meaning in Guardian Notifications API - it is for ease of querying and must be established by
    * convention between clients.
    *
    * e.g., if Breaking News wants to be able to keep track of what notifications it's sent, it should use a type by
    * which it can query the API ('news'). No other clients should use this.
    */
  `type`: NotificationType,
  /** Used for de-duplication */
  uniqueIdentifier: String = UUID.randomUUID.toString,
  sender: String,
  target: Target,
  timeToLiveInSeconds: Int =  60 * 60 * 2,
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
  implicit val jf = Json.writes[Notification]
}