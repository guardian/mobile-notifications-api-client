package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.PayloadBuilder
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.Notification
import PayloadBuilder._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

trait HttpProvider {

  sealed trait HttpResponse
  case class HttpOk(status: Int, body: String) extends HttpResponse {
    require(status >= 200 && status < 300)
  }
  case class HttpError(status: Int, body: String) extends Throwable with HttpResponse
  
  case class ContentType(mediaType: String, charset: String)

  def post(url: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse]
  def get(url: String): Future[HttpResponse]
}

trait ApiClient extends HttpProvider {
  def host: String

  def apiKey: String

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    get(s"$host/healthcheck").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case HttpError(code, _) => Unhealthy(Some(code))
    }
  }

  def send(notification: NotificationPayload)(implicit ec: ExecutionContext): Future[String] = {
    send(buildNotification(notification)) map (_.messageId)
  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    val json = Json.stringify(Json.toJson(notification))
    post(
      url = if (apiKey.isEmpty) s"$host/notifications" else s"$host/notifications?api-key=$apiKey",
      contentType = ContentType("application/json", "UTF-8"),
      body = json.getBytes("UTF-8")
    ) map {
      case HttpOk(code, body) => Json.fromJson[SendNotificationReply](Json.parse(body)).get
      case error: HttpError => throw error
    }
  }
}