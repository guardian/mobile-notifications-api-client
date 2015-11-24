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

  val payload = BreakingNewsPayload(
    title = "myTitle",
    notificationType = BreakingNews.toString,
    message = "myMessage",
    sender = "test sender",
    editions = Set.empty,
    imageUrl = None,
    thumbnailUrl = None,
    link = ExternalLink("http://mylink"),
    importance = Importance.Major,
    topic = Set.empty,
    debug = true
  )

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
      apiKey = apiKey,
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

//TODO remove duplication between legacy and n10n tests
def n10nTest(test: N10nApiClient => Unit):Result = {
  val successServerResponse = HttpOk(200, "{\"messageId\":\"123\"}")
  n10nTest(successServerResponse)(test)
}
  def n10nTest(serverResponse:HttpResponse)(test: N10nApiClient => Unit):Result = {
    val fakeHttpProvider = mock[HttpProvider]
    fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.successful(serverResponse)

    val serviceApi = new N10nApiClient(
      apiKey = apiKey,
      httpProvider = fakeHttpProvider,
      host = legacyHost
    )
    test(serviceApi)

    val bodyCapture = new ArgumentCapture[Array[Byte]]
    val urlCapture = new ArgumentCapture[String]
    val contentTypeCapture = new ArgumentCapture[ContentType]

    there was one(fakeHttpProvider).post(urlCapture, contentTypeCapture, bodyCapture)
    urlCapture.value mustEqual (s"$legacyHost/push?api-key=$apiKey")
    contentTypeCapture.value mustEqual (ContentType("application/json", "UTF-8"))
    new String(bodyCapture.value) mustEqual (payloadAsJson)
  }



  val notificationAsJson = """{"type":"news","uniqueIdentifier":"UNIQUE_ID","sender":"sender","target":{"regions":["uk"],"topics":[{"type":"newsstand","name":"newsstandIos"}]},"timeToLiveInSeconds":10,"payloads":{"ios":{"type":"ios","body":"ios_body","customProperties":{"p1":"v1"},"category":"category"},"android":{"type":"android","body":{"k1":"v1"}}},"metadata":{"m1":"v1"}}"""
  val payloadAsJson= """{"title":"myTitle","notificationType":"news","message":"myMessage","sender":"test sender","editions":[],"link":{"url":"http://mylink"},"importance":"Major","topic":[],"debug":true}"""
//TODO WE NEED TO COVER OTHER API ERRORS
  "LegacyApiClient" should {

    "successfully send legacy notification object" in legacyApiTest {
      legacyApiClient => legacyApiClient.send(notification) must beEqualTo(SendNotificationReply("123")).await
    }
    "successfully send BreakingNewsPayload" in legacyApiTest {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Right(SendNotificationReply("123"))).await
    }
    "return error if cannot send BreakingNewsPayload" in legacyApiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(HttpApiError(status = 500))).await
    }
    "throw error if cannot send legacy notification object" in legacyApiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(notification) must throwA[HttpError].await
    }
  }
  
"n10nApiClient" should {
  "successfully send BreakingNewsPayload" in n10nTest {
    n10nClient => n10nClient.send(payload) must beEqualTo(Right(SendNotificationReply("123"))).await
  }
  "return error if cannot send BreakingNewsPayload" in legacyApiTest(serverResponse = HttpError(500, "")) {
    n10nClient => n10nClient.send(payload) must beEqualTo(Left(HttpApiError(status = 500))).await
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
      val success = SendNotificationReply("123")

      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Left(error1))
      api1.clientId returns "api1"
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(Right(success))
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
      val success = Right(SendNotificationReply("1234"))
      api1.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)
      api2.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)
      api3.send(any[NotificationPayload])(any[ExecutionContext]) returns Future(success)

      val client = new CompositeApiClient(List(api1, api2, api3))
      client.send(payload) must beEqualTo(success).await
    }

  }



}
