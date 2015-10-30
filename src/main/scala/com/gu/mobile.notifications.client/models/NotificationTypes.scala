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
    implicit val jf = new Format[NotificationType] {
      override def reads(json: JsValue): JsResult[NotificationType] = json match {
        case JsString("news") => JsSuccess(BreakingNews)
        case JsString("goal") => JsSuccess(GoalAlert)
        case JsString("content") => JsSuccess(Content)
        case JsString(unkown) => JsError(s"Unkown notification type $unkown")
        case _ => JsError("Invalid json for NotificationType")
      }

      override def writes(nType: NotificationType): JsValue = JsString(nType.toString)
    }
  }
}
