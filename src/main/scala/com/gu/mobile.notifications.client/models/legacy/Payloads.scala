package com.gu.mobile.notifications.client.models.legacy

import play.api.libs.json.Json

sealed trait MessagePayload

case class AndroidMessagePayload(
  body: Map[String, String]
) extends MessagePayload

object AndroidMessagePayload{
  implicit val jf = Json.format[AndroidMessagePayload]
}

case class IOSMessagePayload(
  body: String,
  customProperties: Map[String, String],
  category: Option[String] = None
) extends MessagePayload

object IOSMessagePayload {
  implicit val jf = Json.format[IOSMessagePayload]
}

case class MessagePayloads(
  ios: Option[IOSMessagePayload],
  android: Option[AndroidMessagePayload]
) {
  def isEmpty = ios.isEmpty && android.isEmpty

  /** Platforms available */
  def platforms = Set(ios.map(_ => "ios"), android.map(_ => "android")).flatten
}

object MessagePayloads {
  implicit val jf = Json.format[MessagePayloads]
}