package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions.UK
import com.gu.mobile.notifications.client.models.legacy.{MessagePayloads, Target, Notification}
import com.gu.mobile.notifications.client.models._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import scala.concurrent.Future
import scala.concurrent.duration._

class ApiClientSpec extends Specification with NoTimeConversions {

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
      val notification = BreakingNewsPayload(
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
      val reply = serviceApi.send(notification)
      reply must beEqualTo(SendNotificationReply("123")).await(timeout = 5 seconds)
    }

  }
}
