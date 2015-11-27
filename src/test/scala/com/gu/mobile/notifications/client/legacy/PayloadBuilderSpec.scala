package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions._
import com.gu.mobile.notifications.client.models._
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import com.gu.mobile.notifications.client.legacy.PayloadBuilderImpl._

class PayloadBuilderSpec extends Specification with Mockito {

  val bnp = BreakingNewsPayload(
    id = "someId",
    title = "myTitle",
    `type` = BreakingNews.toString,
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

  "buildNotification" should {
    "throw an exception if the type is ContentAlertPayload" in {
       buildNotification(mock[ContentAlertPayload]) must throwA[UnsupportedOperationException]
    }

    "throw an exception if the type is GoalAlertPayload" in {
      val notif = mock[GoalAlertPayload]
      buildNotification(notif) must throwA[UnsupportedOperationException]
    }

    "return a well constructed Notification if a valid payload is provided" in {
      val notification = buildNotification(bnp)
      notification.uniqueIdentifier mustEqual bnp.id
      notification.`type` mustEqual BreakingNews
      notification.payloads.isEmpty mustNotEqual true
      notification.metadata mustNotEqual Map.empty
      notification.timeToLiveInSeconds must beGreaterThan(0)
      notification.target.topics mustEqual bnp.topic
      notification.target.regions mustEqual (editionsFrom(bnp) flatMap regions.get)
      notification.sender mustEqual "test sender"
    }

    "return a Notification if the type is BreakingNewsPayload" in {
      val notification = buildNotification(bnp)
      notification.`type` mustEqual BreakingNews
    }
  }

}
