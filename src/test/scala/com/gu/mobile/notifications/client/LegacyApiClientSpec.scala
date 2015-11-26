package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.PayloadBuilder
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions.UK
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.execute.Result
import play.api.libs.json.Json

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

  val notification = Notification(
    `type` = BreakingNews,
    uniqueIdentifier = "UNIQUE_ID",
    sender = "sender",
    target = Target(regions = Set(UK), topics = Set(Topic.NewsstandIos)),
    timeToLiveInSeconds = 10,
    payloads = MessagePayloads(Some(iosPayload), Some(androidPayload)),
    metadata = Map("m1" -> "v1")
  )

  val expectedPostBody = Json.stringify(Json.toJson(notification))
  val expectedPostUrl = s"$host/notifications?api-key=$apiKey"

  val fakePayloadBuilder = mock[PayloadBuilder]
  fakePayloadBuilder.buildNotification(payload) returns notification

  override def getTestApiClient(httpProvider: HttpProvider) = new LegacyApiClient(
    apiKey = apiKey,
    httpProvider = httpProvider,
    host = host,
    payloadBuilder = fakePayloadBuilder)

  def apiTest(test: LegacyApiClient => Unit): Result = {
    val successServerResponse = HttpOk(200, "{\"messageId\":\"123\"}")
    apiTest(successServerResponse)(test)
  }

  "LegacyApiClient" should {

    "successfully send legacy notification object" in apiTest {
      legacyApiClient => legacyApiClient.send(notification) must beEqualTo(SendNotificationReply("123")).await
    }
    "successfully send BreakingNewsPayload" in apiTest {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Right(SendNotificationReply("123"))).await
    }
    "return HttpApiError if http error while sending BreakingNewsPayload" in apiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(HttpApiError(status = 500))).await
    }

    "return UnexpectedApiResponseError if legacy returns unexpected json" in apiTest(serverResponse = HttpOk(200, """ {"unexpected" : "yes"} """")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(UnexpectedApiResponseError( """ {"unexpected" : "yes"} """"))).await
    }
    "return UnexpectedApiResponseError if legacy returns invalid json" in apiTest(serverResponse = HttpOk(200, "I'm not valid json at all")) {
      legacyApiClient => legacyApiClient.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("I'm not valid json at all"))).await
    }

    "throw error if error returned while sending legacy notification" in apiTest(serverResponse = HttpError(500, "")) {
      legacyApiClient => legacyApiClient.send(notification) must throwA[HttpError].await
    }
    "Return HttpProviderError if http provider throws exception" in {
      val throwable = new RuntimeException("something went wrong!!")
      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.failed(throwable)
      val legacyClient = getTestApiClient(fakeHttpProvider)
      legacyClient.send(payload) must beEqualTo(Left(HttpProviderError(throwable))).await
    }

  }


}
