package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.{NotificationPayload, SendNotificationReply}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class N10nApiClient(val host: String,
                    val httpProvider: HttpProvider,
                    val clientId: String = "n10n",
                    val endPoint: String = "push",
                    val apiKey: String) extends SimpleHttpApiClient {
  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {
    val json = Json.stringify(Json.toJson(notificationPayload))
    postJson(json) map {
      case error: HttpError => Left(HttpApiError(error.status))
      case HttpOk(code, body) => Right(SendNotificationReply("")) //TODO SEE WHAT TO RETURN HERE, WE PROBABLY NEED TO CHANGE THE MAIN INTERFACE SO THAT IT DOESN'T RETURN AN ID
    } recover {
      case t:Throwable => Left(HttpProviderError(t))
    }
  }

}