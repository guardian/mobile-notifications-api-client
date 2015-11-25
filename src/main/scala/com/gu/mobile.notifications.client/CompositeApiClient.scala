package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.{NotificationPayload, SendNotificationReply}

import scala.concurrent.{ExecutionContext, Future}

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