package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.legacy.Notification
import com.gu.mobile.notifications.client._
import com.gu.mobile.notifications.client.models.{SendNotificationReply, NotificationPayload}
import play.api.libs.json.Json

import scala.concurrent.{Future, ExecutionContext}
class LegacyApiClient(val host: String,
                      val httpProvider: HttpProvider,
                      val apiKey: String,
                      val endPoint: String = "notifications",
                      val clientId: String = "Legacy",
                      payloadBuilder: PayloadBuilder = PayloadBuilderImpl) extends SimpleHttpApiClient {

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {
    val legacyNotification = payloadBuilder.buildNotification(notificationPayload)
    sendToServer(legacyNotification) map {
      case Left(httpError:HttpError) => Left(HttpApiError(httpError.status))
      case Left(t) => Left(HttpProviderError(t))
      case Right(response) => parseResponse(response)
    }

  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    sendToServer(notification) map {
      case Right(response) => parseResponse(response).right.get //TODO will throw exception if this returns an error but this is the way it was before
      case Left(error) => throw error
    }
  }

  private def sendToServer(notification: Notification)(implicit ec: ExecutionContext): Future[Either[Throwable, String]] = {
    val json = Json.stringify(Json.toJson(notification))
    postJson(json) map {
      case HttpOk(code, body) => Right(body)
      case error: HttpError => Left(error)
    } recover {
      case t:Throwable => Left(t)
    }
  }

  private def parseResponse(jsonBody: String): Either[ApiClientError, SendNotificationReply] = {
    try {
      Right(Json.fromJson[SendNotificationReply](Json.parse(jsonBody)).get)
    }
    catch {
      case _: Throwable => Left(UnexpectedApiResponseError(jsonBody))
    }
  }
}