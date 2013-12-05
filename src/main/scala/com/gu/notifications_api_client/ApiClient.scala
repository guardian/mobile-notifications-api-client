package com.gu.notifications_api_client

import com.gu.notifications_api_client.models.{SendNotificationReply, Notification}
import scala.concurrent.Future
import dispatch._
import play.api.libs.json._

trait ApiClient {
  /** Host of the Guardian Notifications Service */
  def host: String

  /** Http client */
  def httpClient: Http

  def send(notification: Notification): Future[SendNotificationReply] = {
    httpClient((url(host) / "notifications") << Json.toJson(notification.toString).toString() OK as.String) map { body =>
      Json.fromJson[SendNotificationReply](Json.parse(body)).get
    }
  }
}
