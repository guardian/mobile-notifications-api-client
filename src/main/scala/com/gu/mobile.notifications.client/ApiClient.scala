package com.gu.mobile.notifications.client

import scala.concurrent.{ExecutionContext, Future}
import dispatch._
import play.api.libs.json._
import models.JsonImplicits._
import lib.const
import models._

trait ApiClient {
  implicit val executionContext: ExecutionContext

  /** Host of the Guardian Notifications Service */
  def host: String

  /** Http client */
  def httpClient: Http

  def healthcheck: Future[Healthcheck] = Http((url(host) / "healthcheck") OK as.String) recover {
    case StatusCode(code) => Unhealthy(code)
  } map const(Ok)

  def send(notification: Notification): Future[SendNotificationReply] = {
    val json = Json.stringify(Json.toJson(notification))
    val request = (url(host) / "notifications").setMethod("POST").setBody(json.getBytes("UTF-8"))
    request.setHeader("Content-Type", "application/json; charset=utf-8")
    httpClient(request OK as.String) map { body =>
      Json.fromJson[SendNotificationReply](Json.parse(body)).get
    }
  }
}
