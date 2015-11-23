package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.{PayloadBuilder, PayloadBuilderImpl}
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.Notification
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}


sealed trait ApiClientError {
  def message: String
}

trait CompositeApiError extends ApiClientError{
  def errors : List[ApiClientError]
  override def message: String = errors.map(_.message).mkString("\n")
}
case class PartialApiError(errors: List[ApiClientError]) extends CompositeApiError
case class TotalApiError(errors: List[ApiClientError]) extends CompositeApiError
case class HttpApiError(status:Int) extends ApiClientError {
  val message = s"Http error status $status"
}



trait ApiClient {
  def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, String]]
}

trait SimpleHttpApiClient extends ApiClient {
  def host: String
  def httpProvider: HttpProvider
  //TODO IS THIS USED FOR ANYTHING ??
  def apiKey: String

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    httpProvider.get(s"$host/healthcheck").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case HttpError(code, _) => Unhealthy(Some(code))
    }
  }
}

class LegacyApiClient(val host: String,
                      val httpProvider: HttpProvider,
                      val apiKey: String,
                      payloadBuilder: PayloadBuilder = PayloadBuilderImpl) extends SimpleHttpApiClient {

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, String]] = {

      val notification = payloadBuilder.buildNotification(notificationPayload)
      this.send(notification) map (x => Right(x.messageId)) recover {case error: HttpError => Left(HttpApiError(error.status))}
  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    val json = Json.stringify(Json.toJson(notification))
    httpProvider.post(
      url = s"$host/notifications",
      contentType = ContentType("application/json", "UTF-8"),
      body = json.getBytes("UTF-8")
    ) map {
      case HttpOk(code, body) => Json.fromJson[SendNotificationReply](Json.parse(body)).get
      case error: HttpError => throw error
    }
  }
}

class N10nApiClient(val host: String,
                    val httpProvider: HttpProvider,
                    val apiKey: String) extends SimpleHttpApiClient {
  //TODO SEE EXACTLY WHAT TO POST HERE AND WHERE AND HOW TO PARSE THE RESULT
  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, String]] = throw new UnsupportedOperationException("Method not implemented")

}

//TODO tag the api clients so that we can return a meaningful error code that can be displayed to the user
class CompositeApiClient(apiClients: List[ApiClient]) extends ApiClient {
  require(apiClients.size > 0)

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, String]] = {
    def sendWithRetry(apiClient: ApiClient) = apiClient.send(notificationPayload)

    val responses = Future.sequence(apiClients.map(sendWithRetry))//TODO make sure there are no future failures here
    responses.map(aggregateResponses)
  }

  //from the responses to the individual call figure out what response to give to the composite api user
  private def aggregateResponses(responses: List[Either[ApiClientError, String]]): Either[ApiClientError, String] = {
    responses.partition(_.isLeft) match {
      case (Nil, rights) => rights(0)
      case (lefts, _) => Left(TotalApiError(lefts.map(_.left.get)))

    }
  }
}