package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.{PayloadBuilder, PayloadBuilderImpl}
import com.gu.mobile.notifications.client.models.legacy.Notification
import com.gu.mobile.notifications.client.models.{NotificationPayload, SendNotificationReply}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

protected class LegacyApiClient(val host: String,
                      val apiKey: String,
                      val httpProvider: HttpProvider,
                      val clientId: String = "Legacy",
                      payloadBuilder: PayloadBuilder = PayloadBuilderImpl) extends SimpleHttpApiClient {

  val endPoint = "notifications"
  private val url = s"$host/$endPoint?api-key=$apiKey"

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {
    val legacyNotification = payloadBuilder.buildNotification(notificationPayload)
    sendToServer(legacyNotification) map {
      case Left(httpError:HttpError) => Left(HttpApiError(httpError.status))
      case Left(t) => Left(HttpProviderError(t))
      case Right(response) => parseResponse(response)
    }

  }

//TODO this is not accessible if this class is protected.. we have to either make this accessible from outside or just refactor this client to
  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    sendToServer(notification) map {
      case Right(response) => parseResponse(response).right.get //TODO will throw exception if this returns an error but this is the way it was before
      case Left(error) => throw error
    }
  }

  private def sendToServer(notification: Notification)(implicit ec: ExecutionContext): Future[Either[Throwable, String]] = {
    val json = Json.stringify(Json.toJson(notification))
    postJson(url, json) map {
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