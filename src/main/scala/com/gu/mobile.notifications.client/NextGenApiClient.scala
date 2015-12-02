package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationPayload
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

case class NextGenResponse(id: String)

object NextGenResponse {
  implicit val jf = Json.format[NextGenResponse]
}

protected class NextGenApiClient(val host: String,
                              val apiKey: String,
                              val httpProvider: HttpProvider,
                              val clientId: String = "nextGen"
                               ) extends SimpleHttpApiClient {


  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, Unit]] = {


    //TODO for now at least we push the notification to the first topic in the payload.. this should be changed after we do MAPI-1123
    notificationPayload.topic.toList match {

      case Nil => Future(Left(MissingParameterError("topic")))

      case firstTopic :: _ => {
        val topicUrlPart = firstTopic.`type` + "/" + firstTopic.name
        val url = s"$host/push/topic/$topicUrlPart?api-key=$apiKey"
        val json = Json.stringify(Json.toJson(notificationPayload))
        postJson(url, json) map {
          case error: HttpError => Left(ApiHttpError(error.status))
          case HttpOk(201, body) => validateFormat[NextGenResponse](body)
          case HttpOk(code, body) => Left(UnexpectedApiResponseError(s"Server returned status code $code and body:$body"))
        } recover {
          case e: Exception => Left(HttpProviderError(e))
        }
      }
    }
  }

}

