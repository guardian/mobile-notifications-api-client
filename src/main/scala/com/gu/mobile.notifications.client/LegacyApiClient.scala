package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.{PayloadBuilder, PayloadBuilderImpl}
import com.gu.mobile.notifications.client.models.{NotificationPayload, SendNotificationReply}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

protected class LegacyApiClient(val host: String,
                                val apiKey: String,
                                val httpProvider: HttpProvider,
                                val clientId: String = "Legacy",
                                payloadBuilder: PayloadBuilder = PayloadBuilderImpl) extends SimpleHttpApiClient {

  private val url = s"$host/notifications?api-key=$apiKey"

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, Unit]] = {
    val legacyNotification = payloadBuilder.buildNotification(notificationPayload)

    val json = Json.stringify(Json.toJson(legacyNotification))
    postJson(url, json) map {
      case HttpOk(code, body) => validateFormat[SendNotificationReply](body)
      case error: HttpError => Left(HttpApiError(error.status))
    } recover {
      case e: Exception => Left(HttpProviderError(e))
    }

  }


}