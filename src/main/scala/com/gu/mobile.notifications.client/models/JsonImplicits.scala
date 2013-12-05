package com.gu.mobile.notifications.client.models

import com.gu.mobile.notifications.client.lib.JsonFormatsHelper._
import play.api.libs.json.{Writes, JsValue, JsString, Json}

object JsonImplicits {
  implicit val androidPayloadWrites = Json.writes[AndroidMessagePayload].withTypeString("android")
  implicit val iosPayloadWrites = Json.writes[IOSMessagePayload].withTypeString("ios")
  implicit val messagePayloadsWrites = Json.writes[MessagePayloads]
  implicit val regionWrites = new Writes[Region] {
    def writes(o: Region): JsValue = JsString(o.toString)
  }
  implicit val topicWrites = Json.writes[Topic]
  implicit val recipientWrites = Json.writes[Recipient]
  implicit val targetWrites = Json.writes[Target]
  implicit val notificationWrites = Json.writes[Notification]

  implicit val sendNotificationsReplyFormat = Json.format[SendNotificationReply]
}
