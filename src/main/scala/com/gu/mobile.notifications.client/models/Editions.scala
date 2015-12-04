package com.gu.mobile.notifications.client.models

import play.api.libs.json._

object Editions {

  val regions: Map[String, Edition] = Map(
    "uk" -> UK,
    "us" -> US,
    "au" -> AU,
    "international" -> International
  )

  sealed trait Edition

  case object UK extends Edition {
    override val toString = "uk"
  }

  case object US extends Edition {
    override val toString = "us"
  }

  case object AU extends Edition {
    override val toString = "au"
  }

  case object International extends Edition {
    override val toString = "international"
  }

  object Edition {
    implicit val jf = new Format[Edition] {
      override def reads(json: JsValue): JsResult[Edition] = json match {
        case JsString("uk") => JsSuccess(UK)
        case JsString("us") => JsSuccess(US)
        case JsString("au") => JsSuccess(AU)
        case JsString("international") => JsSuccess(International)
        case JsString(unknown) => JsError(s"Unkown region [$unknown]")
        case _ => JsError(s"Unknown type $json")
      }

      override def writes(region: Edition): JsValue = JsString(region.toString)
    }
  }
}