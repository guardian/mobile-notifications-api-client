package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.NotificationBuilder
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Editions.UK
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.execute.Result

import scala.concurrent.Future


class LegacyApiClientSpec extends ApiClientSpec[LegacyApiClient] {

  val iosPayload = IOSMessagePayload(
    body = "ios_body",
    customProperties = Map("p1" -> "v1"),
    category = Some("category")
  )
  val androidPayload = AndroidMessagePayload(
    body = Map("k1" -> "v1")
  )

  val payload = BreakingNewsPayload(
    title = "myTitle",
    message = "myMessage",
    sender = "test sender",
    imageUrl = None,
    thumbnailUrl = None,
    link = ExternalLink("http://mylink"),
    importance = Importance.Major,
    topic = Set.empty,
    debug = true
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

  val expectedPostBody = """{"type":"news","uniqueIdentifier":"UNIQUE_ID","sender":"sender","target":{"regions":["uk"],"topics":[{"type":"newsstand","name":"newsstandIos"}]},"timeToLiveInSeconds":10,"payloads":{"ios":{"type":"ios","body":"ios_body","customProperties":{"p1":"v1"},"category":"category"},"android":{"type":"android","body":{"k1":"v1"}}},"metadata":{"m1":"v1"}}"""
  val expectedPostUrl = s"$host/notifications?api-key=$apiKey"

  val fakeNotificationBuilder = mock[NotificationBuilder]
  fakeNotificationBuilder.buildNotification(payload) returns notification

  override def getTestApiClient(httpProvider: HttpProvider) = new LegacyApiClient(
    apiKey = apiKey,
    httpProvider = httpProvider,
    host = host,
    notificationBuilder = fakeNotificationBuilder)

  def apiTest(test: LegacyApiClient => Unit): Result = {
    val successServerResponse = HttpOk(200, "{\"messageId\":\"123\"}")
    apiTest(successServerResponse)(test)
  }

  "LegacyApiClient" should {

    "successfully send BreakingNewsPayload" in apiTest {
      legacyApiClient => legacyApiClient.send(payload) must beRight.await
    }
    "return HttpApiError if http error while sending BreakingNewsPayload" in apiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(ApiHttpError(status = 500))).await
    }

    "return UnexpectedApiResponseError if legacy returns unexpected json" in apiTest(serverResponse = HttpOk(200, "{\"something\":\"else\"}")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("{\"something\":\"else\"}"))).await
    }
    "return UnexpectedApiResponseError if legacy returns invalid json" in apiTest(serverResponse = HttpOk(200, "I'm not valid json at all")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("I'm not valid json at all"))).await
    }

    "return HttpProviderError if http provider throws exception" in {
      val throwable = new RuntimeException("something went wrong!!")
      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.failed(throwable)
      val legacyClient = getTestApiClient(fakeHttpProvider)
      legacyClient.send(payload) must beEqualTo(Left(HttpProviderError(throwable))).await
    }

  }


}
