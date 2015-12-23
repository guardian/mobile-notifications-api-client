package com.gu.mobile.notifications.client.models

import play.api.libs.json._

object NotificationPayloadTypes {
  sealed trait NotificationPayloadType

  case object BreakingNewsType extends NotificationPayloadType {
    override def toString = "news"
  }

  case object ContentAlertType extends NotificationPayloadType {
    override def toString = "content"
  }

  case object GoalAlertType extends NotificationPayloadType {
    override def toString = "goal"
  }

  object NotificationPayloadType {
    implicit val jf = new Writes[NotificationPayloadType] {
      override def writes(nType: NotificationPayloadType): JsValue = JsString(nType.toString)
    }
  }
}
