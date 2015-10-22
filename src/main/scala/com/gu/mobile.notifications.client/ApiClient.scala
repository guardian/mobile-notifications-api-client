package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.legacy.Notification

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import models._

trait HttpProvider {

  sealed trait HttpResponse
  /** For status >= 200 and < 300 */
  case class HttpOk(status: Int, body: String) extends HttpResponse
  /** For every other status */
  case class HttpError(status: Int, body: String) extends Throwable with HttpResponse
  
  case class ContentType(contentType: String, charset: String)

  def http(
    method: String,
    url: String,
    contentType: Option[ContentType] = None,
    body: Option[Array[Byte]] = None
  ): Future[HttpResponse]
}

trait ApiClient extends HttpProvider {
  /** Host of the Guardian Notifications Service */
  def host: String

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    http(url = s"$host/healthcheck", method = "GET").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case httpError => Unhealthy()
    }
  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    val json = Json.stringify(Json.toJson(notification))
    http(
      url = s"$host/notifications",
      method = "POST",
      contentType = Some(ContentType("application/json", "UTF-8")),
      body = Some(json.getBytes("UTF-8"))
    ) map {
      case HttpOk(code, body) => Json.fromJson[SendNotificationReply](Json.parse(body)).get
      case error: HttpError => throw error
    }
  }
}
