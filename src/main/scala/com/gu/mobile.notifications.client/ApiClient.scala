package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.LegacyApiClient
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

case class HttpProviderError(throwable:Throwable) extends ApiClientError{
  def description = throwable.getMessage
}


case class UnexpectedApiResponseError(serverResponse: String) extends ApiClientError {
  val description = s"Unexpected response from server: $serverResponse"
}

trait ApiClient {
  //used to identify the client on error reports
  def clientId: String
  def send(notificationPayload: NotificationPayload)(implicit ec: ExecutionContext): Future[Either[ApiClientError, SendNotificationReply]]
}

trait SimpleHttpApiClient extends ApiClient {
  def host: String
  def endPoint: String
  def httpProvider: HttpProvider
  def apiKey: String

  protected val url = s"$host/$endPoint?api-key=$apiKey"

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


}
//TODO MAYBE PUT THE FACTORY SOMEWHERE ELSE SO THAT THIS IS NOT DEPENDENT ON THE SPECIFIC IMPLEMENTATIONS
object ApiClient {
  def apply(legacyHost: String, legacyApiKey: String, n10nHost: String, n10nApikey: String, httpProvider: HttpProvider): ApiClient = {
    val legacyClient = new LegacyApiClient(host = legacyHost, apiKey = legacyApiKey, httpProvider = httpProvider)
    val n10nClient = new N10nApiClient(host = n10nHost, apiKey = n10nApikey, httpProvider = httpProvider)
    new CompositeApiClient(List(n10nClient, legacyClient))
  }
}