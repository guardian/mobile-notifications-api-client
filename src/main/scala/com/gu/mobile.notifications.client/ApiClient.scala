package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models._

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

case class HttpProviderError(throwable: Throwable) extends ApiClientError {
  def description = throwable.getMessage
}


case class UnexpectedApiResponseError(serverResponse: String) extends ApiClientError {
  val description = s"Unexpected response from server: $serverResponse"
}

case class MissingParameterError(parameterName: String) extends ApiClientError {
  val description = s"No value provided for parameter: $parameterName"
}

trait ApiClient {
  //used to identify the client on error reports
  def clientId: String

  def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]]
}

protected trait SimpleHttpApiClient extends ApiClient {
  def host: String

  def endPoint: String

  def httpProvider: HttpProvider

  def apiKey: String

  def healthcheck(implicit ec: ExecutionContext): Future[Healthcheck] = {
    httpProvider.get(s"$host/healthcheck").map {
      case HttpOk(200, body) => Ok
      case HttpOk(code, _) => Unhealthy(Some(code))
      case HttpError(code, _) => Unhealthy(Some(code))
    }
  }

  protected def postJson(destUrl: String, json: String) = {
    httpProvider.post(
      url = destUrl,
      contentType = ContentType("application/json", "UTF-8"),
      body = json.getBytes("UTF-8")
    )
  }

}

object ApiClient {
  def apply(legacyHost: Option[String] = None, legacyApiKey: Option[String] = None, n10nHost: String, n10nApikey: String, httpProvider: HttpProvider): ApiClient = {
    val n10nClient = new N10nApiClient(host = n10nHost, apiKey = n10nApikey, httpProvider = httpProvider)

    (legacyHost, legacyApiKey) match {
      case (Some(legacyHostVal), Some(legacyKeyVal)) => {
        val legacyClient = new LegacyApiClient(host = legacyHostVal, apiKey = legacyKeyVal, httpProvider = httpProvider)
        new CompositeApiClient(List(n10nClient, legacyClient))
      }
      case (_, _) => n10nClient
    }

  }


}

