package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.{PayloadBuilder, PayloadBuilderImpl}
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.Notification
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}


sealed trait ApiClientError {
  def description: String
}

case class ErrorWithSource(clientId: String, error: ApiClientError) {
  def description = clientId + ": " + error.description
}

trait CompositeApiError extends ApiClientError {
  def errors: List[ErrorWithSource]

  override def description: String = errors.map(e => e.description).mkString(", ")
}

case class PartialApiError(errors: List[ErrorWithSource]) extends CompositeApiError

case class TotalApiError(errors: List[ErrorWithSource]) extends CompositeApiError

case class HttpApiError(status: Int) extends ApiClientError {
  val description = s"Http error status $status"
}

trait ApiClient {
  //used to identify the client on error reports
  def clientId: String

  def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]]
}

trait SimpleHttpApiClient extends ApiClient {
  def host: String

  def httpProvider: HttpProvider

  def apiKey: String
  protected val url = s"$host/notifications?api-key=$apiKey"

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    httpProvider.get(s"$host/healthcheck").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case HttpError(code, _) => Unhealthy(Some(code))
    }
  }

  protected def postJson(json: String) = {
    httpProvider.post(
      url = this.url,
      contentType = ContentType("application/json", "UTF-8"),
      body = json.getBytes("UTF-8")
    )
  }

  protected def parseResponse(jsonBody: String) = Json.fromJson[SendNotificationReply](Json.parse(jsonBody)).get
}

class LegacyApiClient(val host: String,
                      val httpProvider: HttpProvider,
                      val apiKey: String,
                      val clientId: String = "Legacy",
                      payloadBuilder: PayloadBuilder = PayloadBuilderImpl) extends SimpleHttpApiClient {

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {
    val legacyNotification = payloadBuilder.buildNotification(notificationPayload)
    sendToServer(legacyNotification) map {
      case Left(httpError) => Left(HttpApiError(httpError.status))
      case Right(sendNotificationReply) => Right(sendNotificationReply)
    }
  }

  def send(notification: Notification)(implicit ec: ExecutionContext): Future[SendNotificationReply] = {
    sendToServer(notification) map {
      case Right(sendNotificationReply) => sendNotificationReply
      case Left(error) => throw error
    }
  }

  private def sendToServer(notification: Notification)(implicit ec: ExecutionContext): Future[Either[HttpError, SendNotificationReply]] = {
    val json = Json.stringify(Json.toJson(notification))
    postJson(json) map {
      case HttpOk(code, body) => Right(parseResponse(body))
      case error: HttpError => Left(error)
    }
  }
}

class N10nApiClient(val host: String,
                    val httpProvider: HttpProvider,
                    val clientId: String = "n10n",
                    val apiKey: String) extends SimpleHttpApiClient {
  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {
    //TODO
    // val json = Json.stringify(Json.toJson(notificationPayload))
    val json = "{some json}"
    postJson(json) map {
      case error: HttpError => Left(HttpApiError(error.status))
      case HttpOk(code, body) => Right(parseResponse(body))
    }

  }
}

class CompositeApiClient(apiClients: List[ApiClient], val clientId: String = "composite") extends ApiClient {
  require(apiClients.size > 0)

  override def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]] = {

    def sendAndSource(apiClient: ApiClient): Future[Either[ErrorWithSource, SendNotificationReply]] = {
      apiClient.send(notificationPayload) map {
        case Left(error) => Left(ErrorWithSource(apiClient.clientId, error))
        case Right(x) => Right(x)
      }
    }

    val responses = Future.sequence(apiClients.map(sendAndSource)) //TODO make sure there are no future failures here (but how?)
    responses.map(aggregateResponses)
  }

  //from the responses to the individual calls figure out what response to give to the composite api user
  private def aggregateResponses(responses: List[Either[ErrorWithSource, SendNotificationReply]]): Either[ApiClientError, SendNotificationReply] = {
    responses.partition(_.isLeft) match {
      case (Nil, Right(res) :: _) => Right(res)
      case (lefts, Nil) => Left(TotalApiError(lefts.map(_.left.get)))
      case (lefts, _) => Left(PartialApiError(lefts.map(_.left.get)))

    }
  }
}

object ApiClient {
  def apply(legacyHost: String, legacyApiKey: String, n10nHost: String, n10nApikey: String, httpProvider: HttpProvider): ApiClient = {
    val legacyClient = new LegacyApiClient(host = legacyHost, apiKey = legacyApiKey, httpProvider = httpProvider)
    val n10nClient = new N10nApiClient(host = n10nHost, apiKey = n10nApikey, httpProvider = httpProvider)
    new CompositeApiClient(List(n10nClient, legacyClient))
  }
}