package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Priority.Major
import com.gu.mobile.notifications.client.models.Regions._
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.Notification
import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import com.gu.mobile.notifications.client.legacy.PayloadBuilder._

class PayloadBuilderSpec extends Specification with Mockito {

  "buildNotification" should {
    "throw an exception if the type is ContentAlertPayload" in {
       buildNotification(mock[ContentAlertPayload]) must throwA[UnsupportedOperationException]
    }

    "throw an exception if the type is GoalAlertPayload" in {
      val notif = mock[GoalAlertPayload]
      buildNotification(notif) must throwA[UnsupportedOperationException]
    }

    "return a well constructed Notification if a valid payload is provided" in {
      val bnp = mock[BreakingNewsPayload]
      bnp.title returns "myTitle"
      bnp.notificationType returns "news"
      bnp.message returns "myMessage"
      bnp.sender returns "test sender"
      bnp.editions returns Set.empty
      bnp.imageUrl returns None
      bnp.thumbnailUrl returns None
      bnp.link returns mock[ExternalLink]
      bnp.priority returns Major
      bnp.topic returns Set.empty
      bnp.debug returns true
      val notification = buildNotification(bnp)
      notification.`type` mustEqual BreakingNews
      notification.payloads.isEmpty mustNotEqual true
      notification.metadata mustNotEqual Map.empty
      notification.timeToLiveInSeconds must beGreaterThan(0)
      notification.target.topics mustEqual bnp.topic
      notification.target.regions mustEqual (editionsFrom(bnp) flatMap regions.get)
      notification.sender mustEqual "test sender"
    }

    "return a Notification if the type is BreakingNewsPayload" in {
      val bnp = mock[BreakingNewsPayload]
      bnp.title returns "myTitle"
      bnp.notificationType returns "news"
      bnp.message returns "myMessage"
      bnp.sender returns "test sender"
      bnp.editions returns Set.empty
      bnp.imageUrl returns None
      bnp.thumbnailUrl returns None
      bnp.link returns mock[ExternalLink]
      bnp.priority returns Major
      bnp.topic returns Set.empty
      bnp.debug returns true
      val notification = buildNotification(bnp)
      notification.`type` mustEqual BreakingNews
    }
  }

}
