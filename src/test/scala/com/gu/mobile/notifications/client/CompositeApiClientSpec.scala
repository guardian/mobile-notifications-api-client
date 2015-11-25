package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.{BreakingNewsPayload, SendNotificationReply, NotificationPayload}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.{Future, ExecutionContext}

class CompositeApiClientSpec  extends Specification with Mockito with NoTimeConversions{
  "CompositeApiClient" should {
    val payload = mock[BreakingNewsPayload]

    "Report total error if all api calls fail" in {
      val api1 = mock[ApiClient]
      val api2 = mock[ApiClient]
      val api3 = mock[ApiClient]

      val error1 = HttpApiError(500)
      val error2 = HttpApiError(400)
      val error3 = HttpApiError(403)

      api1.clientId returns "api1"
      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error1))
      api2.clientId returns "api2"
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error2))
      api3.clientId returns "api3"
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error3))
      val client = new CompositeApiClient(List(api1, api2, api3))

      val sourcedErrorList = List(ErrorWithSource("api1", error1), ErrorWithSource("api2", error2), ErrorWithSource("api3", error3))

      client.send(payload) must beEqualTo(Left(TotalApiError(sourcedErrorList))).await
    }

    "Report partial error if some of the api calls fail" in {
      val api1 = mock[ApiClient]
      val api2 = mock[ApiClient]
      val api3 = mock[ApiClient]

      val error1 = HttpApiError(500)
      val error3 = HttpApiError(403)
      val success = SendNotificationReply("123")

      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error1))
      api1.clientId returns "api1"
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Right(success))
      api2.clientId returns "api2"
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error3))
      api3.clientId returns "api3"

      val client = new CompositeApiClient(List(api1, api2, api3))

      val sourcedErrorList = List(ErrorWithSource("api1", error1), ErrorWithSource("api3", error3))
      client.send(payload) must beEqualTo(Left(PartialApiError(sourcedErrorList))).await
    }

    "Return first api response on success" in {

      val api1 = mock[ApiClient]
      val api2 = mock[ApiClient]
      val api3 = mock[ApiClient]
      val success = Right(SendNotificationReply("1234"))
      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)

      val client = new CompositeApiClient(List(api1, api2, api3))
      client.send(payload) must beEqualTo(success).await
    }

  }
}
