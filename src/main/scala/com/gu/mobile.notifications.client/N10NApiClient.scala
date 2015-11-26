package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.{NotificationPayload, SendNotificationReply}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

protected class N10nApiClient(val host: String,
                              val apiKey: String,
                              val httpProvider: HttpProvider,
                              val clientId: String = "n10n"
                               ) extends SimpleHttpApiClient {

  val endPoint = "push"

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {

    if (notificationPayload.topic.isEmpty) return Future(Left(MissingParameterError("topic")))

    //TODO for now at least we push the notification to the first topic in the payload.. this should be changed after we do MAPI-1123
    val topicName = notificationPayload.topic.head.name

    val url = s"$host/$endPoint/$topicName?api-key=$apiKey"
    val json = Json.stringify(Json.toJson(notificationPayload))
    postJson(url, json) map {
      case error: HttpError => Left(HttpApiError(error.status))
      case HttpOk(code, body) => Right(SendNotificationReply("")) //TODO SEE WHAT TO RETURN HERE, WE PROBABLY NEED TO CHANGE THE MAIN INTERFACE SO THAT IT DOESN'T RETURN AN ID
    } recover {
      case t: Throwable => Left(HttpProviderError(t))
    }
  }

}