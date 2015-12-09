package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.legacy.NotificationBuilderImpl._
import com.gu.mobile.notifications.client.models.Editions._
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.{AndroidMessagePayload, IOSMessagePayload}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class NotificationBuilderSpec extends Specification with Mockito {

  val bnp = BreakingNewsPayload(
    id = "someId",
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

  val bnpGuardianLink = BreakingNewsPayload(
    id = "someId",
    title = "myTitle",
    message = "myMessage",
    sender = "test sender",
    imageUrl = None,
    thumbnailUrl = None,
    link = GuardianLinkDetails("contentId", Some("gu.com/p/3tx47"), "myTitle", None, GITContent),
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
      val expectedAndroidPayload = AndroidMessagePayload(
        Map("topics" -> "",
          "editions" -> "",
          "debug" -> "true",
          "notificationType" -> "news",
          "link" -> "http://mylink",
          "message" -> "myMessage",
          "title" -> "myTitle",
          "type" -> "custom",
          "ticker" -> "myMessage"
        )
      )

      val expectedIosPayload = IOSMessagePayload(
        body = "myMessage",
        customProperties = Map("t" -> "m", "notificationType" -> "news", "link" -> "http://mylink", "topics" -> ""),
        category = None
      )

      val notification = buildNotification(bnp)
      notification.uniqueIdentifier mustEqual bnp.id
      notification.`type` mustEqual BreakingNews
      notification.payloads.isEmpty mustNotEqual true
      notification.metadata mustNotEqual Map.empty
      notification.timeToLiveInSeconds must beGreaterThan(0)
      notification.target.topics mustEqual bnp.topic
      notification.target.regions mustEqual Set.empty
      notification.sender mustEqual "test sender"
      notification.payloads.android.get mustEqual expectedAndroidPayload
      notification.payloads.ios.get mustEqual expectedIosPayload
    }

    "return the correct link format for each platform" in {
      val expectedAndroidPayload = AndroidMessagePayload(
        Map("topics" -> "",
          "editions" -> "",
          "debug" -> "true",
          "notificationType" -> "news",
          "link" -> "x-gu://www.guardian.co.uk/contentId",
          "message" -> "myMessage",
          "title" -> "myTitle",
          "type" -> "custom",
          "ticker" -> "myMessage"
        )
      )

      val expectedIosPayload = IOSMessagePayload(
        body = "myMessage",
        customProperties = Map("t" -> "m", "notificationType" -> "news", "link" -> "x-gu://gu.com/p/3tx47", "topics" -> ""),
        category = Some("ITEM_CATEGORY")
      )

      val notification = buildNotification(bnpGuardianLink)
      notification.uniqueIdentifier mustEqual bnpGuardianLink.id
      notification.`type` mustEqual BreakingNews
      notification.payloads.isEmpty mustNotEqual true
      notification.payloads.android.get mustEqual expectedAndroidPayload
      notification.payloads.ios.get mustEqual expectedIosPayload
    }

    "convert topics to editions" in {
      val breakingNewsWithTopics = BreakingNewsPayload(
        id = "someId",
        title = "myTitle",
        message = "myMessage",
        sender = "test sender",
        imageUrl = None,
        thumbnailUrl = None,
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = Set(BreakingNewsUk, BreakingNewsUs, BreakingNewsSport, NewsstandIos),
        debug = true
      )

      val notification = buildNotification(breakingNewsWithTopics)
      notification.target.topics mustEqual Set(BreakingNewsSport, NewsstandIos)
      notification.target.regions mustEqual Set(UK,US)

      val androidBody = notification.payloads.android.get.body
      androidBody("topics") mustEqual("breaking//sport,newsstand//newsstandIos")
      androidBody("editions") mustEqual("uk,us")
    }

    "return a Notification if the type is BreakingNewsPayload" in {
      val notification = buildNotification(bnp)
      notification.`type` mustEqual BreakingNews
    }
  }

}
