package com.gu.mobile.notifications.client.models

import play.api.libs.json._

object NotificationTypes {
  sealed trait NotificationType

  case object BreakingNews extends NotificationType {
    override def toString = "news"
  }

  case object Content extends NotificationType {
    override def toString = "content"
  }

  case object GoalAlert extends NotificationType {
    override def toString = "goal"
  }

  object NotificationType {
    implicit val jf = new Writes[NotificationType] {
      override def writes(nType: NotificationType): JsValue = JsString(nType.toString)
    }
  }
}
