package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.legacy.PayloadBuilder
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Priority.Major
import com.gu.mobile.notifications.client.models.Regions.UK
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.ArgumentCapture
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.Future
import scala.concurrent.duration._

class ApiClientSpec extends Specification with Mockito with NoTimeConversions {
  val legacyHost = "http://legacyHost.co.uk"

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

  //TODO I WOULD PREFER TO MOCK OUT THE JSON GENERATION AND TEST THAT ON ITS OWN TEST
  val notificationAsJson =
    """{"type":"news","uniqueIdentifier":"UNIQUE_ID","sender":"sender","target":{"regions":["uk"],"topics":[{"type":"newsstand","name":"newsstandIos"}]},"timeToLiveInSeconds":10,"payloads":{"ios":{"type":"ios","body":"ios_body","customProperties":{"p1":"v1"},"category":"category"},"android":{"type":"android","body":{"k1":"v1"}}},"metadata":{"m1":"v1"}}"""


  "LegacyApiClient" should {

    //TODO EXTRACT DUPLICATED CODE INTO SOME COMMON PLACE
    "successfully send notification" in {

      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.successful(HttpOk(200, "{\"messageId\":\"123\"}"))

      val serviceApi = new LegacyApiClient {
        val apiKey = "IS THIS USED FOR ANYTHING?"
        val httpProvider = fakeHttpProvider
        val host = legacyHost
      }

      val reply = serviceApi.send(notification)
      reply must beEqualTo(SendNotificationReply("123")).await(timeout = 5 seconds)
      val bodyCapture = new ArgumentCapture[Array[Byte]]
      val urlCapture = new ArgumentCapture[String]
      val contentTypeCapture = new ArgumentCapture[ContentType]

      there was one(fakeHttpProvider).post(urlCapture, contentTypeCapture, bodyCapture)
      urlCapture.value mustEqual (s"$legacyHost/notifications")
      contentTypeCapture.value mustEqual (ContentType("application/json", "UTF-8"))
      new String(bodyCapture.value) mustEqual (notificationAsJson)
    }

    "successfully send BreakingNewsPayload" in {
      //the values in this payload object don't matter as the payloadBuilder used to convert it to a notification object is faked anyway...
      val payload = BreakingNewsPayload(
        title = "myTile",
        message = "msg",
        thumbnailUrl = None,
        sender = "sender",
        editions = Set.empty,
        link = ExternalLink("url"),
        imageUrl = None,
        priority = Major,
        topic = Set.empty,
        debug = false)

      val fakePayloadBuilder = mock[PayloadBuilder]
      fakePayloadBuilder.buildNotification(payload) returns notification

      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.successful(HttpOk(200, "{\"messageId\":\"123\"}"))

      val serviceApi = new LegacyApiClient {
        val apiKey = "IS THIS USED FOR ANYTHING?"
        val httpProvider = fakeHttpProvider
        val host = legacyHost
        override val payloadBuilder = fakePayloadBuilder
      }

      val reply = serviceApi.send(payload)
      reply must beEqualTo("123").await(timeout = 5 seconds)
      val bodyCapture = new ArgumentCapture[Array[Byte]]
      val urlCapture = new ArgumentCapture[String]
      val contentTypeCapture = new ArgumentCapture[ContentType]

      there was one(fakeHttpProvider).post(urlCapture, contentTypeCapture, bodyCapture)
      urlCapture.value mustEqual (s"$legacyHost/notifications")
      contentTypeCapture.value mustEqual (ContentType("application/json", "UTF-8"))
      new String(bodyCapture.value) mustEqual (notificationAsJson)
    }

  }

}
