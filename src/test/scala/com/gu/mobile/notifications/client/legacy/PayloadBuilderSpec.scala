package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions.{US, UK}
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

    "throw an exception if the type is ContentAlertPayload" in {
      val notif = mock[GoalAlertPayload]
      buildNotification(notif) must throwA[UnsupportedOperationException]
    }

    "return a Notification if the type is BreakingNews" in {
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
      buildNotification(notif).`type` mustEqual BreakingNews
    }
  }

}
