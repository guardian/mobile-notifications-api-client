package com.gu.mobile.notifications.client.models

import play.api.libs.json.Json

object Topic { implicit val jf = Json.format[Topic] }
case class Topic(
  `type`: String,
  name: String
) {
  override def toString = s"${`type`}/$name"
}
