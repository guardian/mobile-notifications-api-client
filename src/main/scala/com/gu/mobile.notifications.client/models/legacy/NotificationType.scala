package com.gu.mobile.notifications.client.models.legacy

import play.api.libs.json._

sealed trait NotificationType

object NotificationType {
  implicit val jf = new Writes[NotificationType] {
    override def writes(nType: NotificationType): JsValue = JsString(nType.toString)
  }

  case object BreakingNews extends NotificationType {
    override def toString = "news"
  }

  case object Content extends NotificationType {
    override def toString = "tag"
  }

  case object GoalAlert extends NotificationType {
    override def toString = "goal"
  }

}

