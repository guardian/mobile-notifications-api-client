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
  
  case class ContentType(mediaType: String, charset: String)

  def post(url: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse]
  def get(url: String): Future[HttpResponse]
}

trait ApiClient extends HttpProvider {
  /** Host of the Guardian Notifications Service */
  def host: String

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    get(s"$host/healthcheck").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case httpError => Unhealthy()
    }
  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    val json = Json.stringify(Json.toJson(notification))
    post(
      url = s"$host/notifications",
      contentType = ContentType("application/json", "UTF-8"),
      body = json.getBytes("UTF-8")
    ) map {
      case HttpOk(code, body) => Json.fromJson[SendNotificationReply](Json.parse(body)).get
      case error: HttpError => throw error
    }
  }
}
