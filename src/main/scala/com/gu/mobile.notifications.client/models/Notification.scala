package com.gu.mobile.notifications.client.models

/** Models for interfacing with the services API's JSON endpoint */
sealed trait MessagePayload

/** Android's message payload is just a map of properties - the client determines how to display that */
case class AndroidMessagePayload(
  body: Map[String, String]
) extends MessagePayload

/** IOS message payload is a String body and a map of custom properties that the app can use */
case class IOSMessagePayload(
  body: String,
  customProperties: Map[String, String],
  category: Option[String] = None
) extends MessagePayload

case class MessagePayloads(
  ios: Option[IOSMessagePayload],
  android: Option[AndroidMessagePayload]
) {
  def isEmpty = ios.isEmpty && android.isEmpty

  /** Platforms available */
  def platforms = Set(ios.map(_ => "ios"), android.map(_ => "android")).flatten
}

case class Topic(
  `type`: String,
  name: String
)

sealed trait Region

case object UK extends Region {
  override def toString = "uk"
}

case object US extends Region {
  override def toString = "us"
}

case object AU extends Region {
  override def toString = "au"
}

case class Target(
  regions: Set[Region],
  topics: Set[Topic],
  recipients: Option[Map[String, Seq[Recipient]]] = None
)

case class Recipient(userId: String)

case class Notification(
  /** Type has no meaning in Guardian Notifications API - it is for ease of querying and must be established by
    * convention between clients.
    *
    * e.g., if Breaking News wants to be able to keep track of what notifications it's sent, it should use a type by
    * which it can query the API ('news'). No other clients should use this.
    */
  `type`: String,
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