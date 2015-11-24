package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Importance.Major
import com.gu.mobile.notifications.client.models.Regions.UK
import com.gu.mobile.notifications.client.models.legacy.{MessagePayloads, Target, Notification}
import com.gu.mobile.notifications.client.models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import scala.concurrent.Future
import scala.concurrent.duration._

class ApiClientSpec extends Specification with Mockito with NoTimeConversions {

  "ApiClient" should {

    val serviceApi = new ApiClient {
      def apiKey = "myKey"
      def host = "myHost"
      def get(url: String): Future[HttpResponse] = Future.successful(HttpError(500, "Not implemented"))
      def post(url: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
        Future.successful(HttpOk(200, "{\"messageId\":\"123\"}"))
      }
    }

    "successfully send if provided with a Notification" in {
      val notification = Notification(
        `type` = BreakingNews,
        sender = "mySender",
        target = Target(Set(UK), Set.empty),
        payloads = MessagePayloads(None, None),
        metadata = Map.empty
      )
      val reply = serviceApi.send(notification)
      reply must beEqualTo(SendNotificationReply("123")).await(timeout = 5 seconds)
    }

    "successfully send if provided with a BreakingNewsPayload" in {
      val notif = mock[BreakingNewsPayload]
      notif.title returns "myTitle"
      notif.notificationType returns "news"
      notif.message returns "myMessage"
      notif.sender returns "test sender"
      notif.editions returns Set.empty
      notif.imageUrl returns None
      notif.thumbnailUrl returns None
      notif.link returns mock[ExternalLink]
      notif.priority returns Major
      notif.topic returns Set.empty
      notif.debug returns true
      val reply = serviceApi.send(notif)
      reply must beEqualTo("123").await(timeout = 5 seconds)
    }

  }
}
