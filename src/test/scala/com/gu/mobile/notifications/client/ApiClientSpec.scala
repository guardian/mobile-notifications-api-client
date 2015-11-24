package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.PayloadBuilder
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions.UK
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.{ExecutionContext, Future}


class ApiClientSpec extends Specification with Mockito with NoTimeConversions {
  val legacyHost = "http://legacyHost.co.uk"
  val apiKey = "apiKey"

  val iosPayload = IOSMessagePayload(
    body = "ios_body",
    customProperties = Map("p1" -> "v1"),
    category = Some("category")
  )
  val androidPayload = AndroidMessagePayload(
    body = Map("k1" -> "v1")
  )

  val notification = Notification(
    `type` = BreakingNews,
    uniqueIdentifier = "UNIQUE_ID",
    sender = "sender",
    target = Target(regions = Set(UK), topics = Set(Topic.NewsstandIos)),
    timeToLiveInSeconds = 10,
    payloads = MessagePayloads(Some(iosPayload), Some(androidPayload)),
    metadata = Map("m1" -> "v1")
  )

  val payload = mock[BreakingNewsPayload]
  val fakePayloadBuilder = mock[PayloadBuilder]
  fakePayloadBuilder.buildNotification(payload) returns notification

  def legacyApiTest(test: LegacyApiClient => Unit):Result = {
    val successServerResponse = HttpOk(200, "{\"messageId\":\"123\"}")
    legacyApiTest(successServerResponse)(test)
  }

  def legacyApiTest(serverResponse:HttpResponse)(test: LegacyApiClient => Unit):Result = {
    val fakeHttpProvider = mock[HttpProvider]
    fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.successful(serverResponse)

    val serviceApi = new LegacyApiClient(
      apiKey = Some(apiKey),
      httpProvider = fakeHttpProvider,
      host = legacyHost,
      payloadBuilder = fakePayloadBuilder
    )
    test(serviceApi)

    val bodyCapture = new ArgumentCapture[Array[Byte]]
    val urlCapture = new ArgumentCapture[String]
    val contentTypeCapture = new ArgumentCapture[ContentType]

    there was one(fakeHttpProvider).post(urlCapture, contentTypeCapture, bodyCapture)
    urlCapture.value mustEqual (s"$legacyHost/notifications?api-key=$apiKey")
    contentTypeCapture.value mustEqual (ContentType("application/json", "UTF-8"))
    new String(bodyCapture.value) mustEqual (notificationAsJson)
  }

  val notificationAsJson = """{"type":"news","uniqueIdentifier":"UNIQUE_ID","sender":"sender","target":{"regions":["uk"],"topics":[{"type":"newsstand","name":"newsstandIos"}]},"timeToLiveInSeconds":10,"payloads":{"ios":{"type":"ios","body":"ios_body","customProperties":{"p1":"v1"},"category":"category"},"android":{"type":"android","body":{"k1":"v1"}}},"metadata":{"m1":"v1"}}"""

//TODO WE NEED TO COVER OTHER API ERRORS
  "LegacyApiClient" should {

    "successfully send legacy notification object" in legacyApiTest {
      legacyApiClient => legacyApiClient.send(notification) must beEqualTo(SendNotificationReply("123")).await
    }
    "successfully send BreakingNewsPayload" in legacyApiTest {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Right("123")).await
    }
    "return error if cannot send BreakingNewsPayload" in legacyApiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(HttpApiError(status = 500))).await
    }
    "throw error if cannot send legacy notification object" in legacyApiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(notification) must throwA[HttpError].await
    }
  }

  "CompositeApiClient" should {
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

      val sourcedErrorList = List(ErrorWithSource("api1", error1), ErrorWithSource("api2", error2), ErrorWithSource("api3", error3) )

      client.send(payload) must beEqualTo(Left(TotalApiError(sourcedErrorList))).await

    }

    "Report partial error if some of the api calls fail" in {
      val api1 = mock[ApiClient]
      val api2 = mock[ApiClient]
      val api3 = mock[ApiClient]

      val error1 = HttpApiError(500)
      val error3 = HttpApiError(403)

      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error1))
      api1.clientId returns "api1"
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Right("{successful json response would go here}"))
      api2.clientId returns "api2"
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error3))
      api3.clientId returns "api3"

      val client = new CompositeApiClient(List(api1, api2, api3))

      val sourcedErrorList = List(ErrorWithSource("api1", error1), ErrorWithSource("api3", error3) )
      client.send(payload) must beEqualTo(Left(PartialApiError(sourcedErrorList))).await


    }

    "Return first api response on success" in {
      val api1 = mock[ApiClient]
      val api2 = mock[ApiClient]
      val api3 = mock[ApiClient]

      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns  Future(Right("Api_1_response"))
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Right("Api_2_response"))
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns  Future(Right("Api_3_response"))

      val client = new CompositeApiClient(List(api1, api2, api3))

      client.send(payload) must beEqualTo(Right("Api_1_response")).await
    }

  }

}
