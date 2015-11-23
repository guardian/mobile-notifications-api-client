package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.{PayloadBuilder, PayloadBuilderImpl}
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.Notification
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}


//TODO ARE THERE ERROR CASES BESIDES HTTP ERRORS?. LIKE THE API RETURNING AN ERROR BECAUSE THERE IS SOMETHING MISSING IN THE JSON IMPUT OR SOMETHING IS WRONG?
sealed trait ApiClientError {
  //todo probably some more details here
  def message: String
}

//TODO how much information we need here? just to know if it was full or partial error and a human readable description is enough?
case class TotalError(message: String) extends ApiClientError
case class PartialError(message: String) extends ApiClientError

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
      this.send(notification) map (x => Right(x.messageId)) recover {case error: HttpError => Left(TotalError(error.getMessage))}

    //  case error: HttpError => Future(Left(TotalError(error.getMessage)))

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
  require(apiClients.size>0)

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, String]] = {
    def sendWithRetry(apiClient: ApiClient) = apiClient.send(notificationPayload)

    val responses = Future.sequence(apiClients.map(sendWithRetry))
    responses.map(aggregateResponses)
  }

  //from the responses to the individual call figure out what response to give to the composite api user
  private def aggregateResponses(responses: List[Either[ApiClientError, String]]): Either[ApiClientError, String] = {
    val (failures, success) = responses.partition(_.isLeft)
    //TODO ugly code here, refactor later
    if (failures.isEmpty)
      success(0) //according to diego all responses from all apis should be the same so I just grab the first one
    else {
      val errorMessage = failures.map(_.left.get.message).mkString("\n")
      if (success.isEmpty)
        Left(TotalError(errorMessage))
      else
        Left(PartialError(errorMessage))
    }
  }

}