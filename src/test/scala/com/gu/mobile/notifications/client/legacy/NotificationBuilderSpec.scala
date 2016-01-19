package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.legacy.NotificationBuilderImpl._
import com.gu.mobile.notifications.client.models.Editions._
import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models.legacy.{NotificationType, AndroidMessagePayload, IOSMessagePayload, Target}
import com.gu.mobile.notifications.client.models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class NotificationBuilderSpec extends Specification with Mockito {

  "buildNotification" should {
    "return a well constructed Notification if a valid ContentAlertPayload is provided" in new ContentAlertScope {
      val notification = buildNotification(cap)

      notification.uniqueIdentifier mustEqual "contentNotifications/newArticle/capiId"
      notification.`type` mustEqual NotificationType.Content
      notification.sender mustEqual cap.sender
      notification.target mustEqual Target(Set.empty, cap.topic)
      notification.metadata mustEqual expectedMetadata
      notification.timeToLiveInSeconds must beGreaterThan(0)
      notification.target.topics mustEqual cap.topic
      notification.target.regions mustEqual Set.empty
      notification.sender mustEqual "mySender"
      notification.payloads.android.get mustEqual expectedAndroidPayload
      notification.payloads.ios.get mustEqual expectedIosPayload
      notification.importance mustEqual Importance.Minor
    }

    "compute an ID for an article" in new ContentAlertScope {
      val notification = buildNotification(cap)
      notification.uniqueIdentifier mustEqual "contentNotifications/newArticle/capiId"
    }

    "compute an ID for a liveblog block" in new ContentAlertScope {
      val content = cap.copy(link = link.copy(blockId = Some("block-abcdefgh")))
      val notification = buildNotification(content)
      notification.uniqueIdentifier mustEqual "contentNotifications/newBlock/capiId/block-abcdefgh"
    }

    "throw an exception if the type is GoalAlertPayload" in {
      val notif = mock[GoalAlertPayload]
      buildNotification(notif) must throwA[UnsupportedOperationException]
    }

    "return a well constructed Notification if a valid breaking news payload is provided" in new BreakingNewsScope {
      val notification = buildNotification(bnp)

      notification.uniqueIdentifier mustEqual bnp.id
      notification.`type` mustEqual NotificationType.BreakingNews
      notification.payloads.isEmpty mustNotEqual true
      notification.metadata mustNotEqual Map.empty
      notification.timeToLiveInSeconds must beGreaterThan(0)
      notification.target.topics mustEqual bnp.topic
      notification.target.regions mustEqual Set.empty
      notification.sender mustEqual "test sender"
      notification.payloads.android.get mustEqual expectedAndroidPayload
      notification.payloads.ios.get mustEqual expectedIosPayload
      notification.importance mustEqual Importance.Major
    }

    "return the correct link format for each platform" in new PlatFormLinkTestScope {

      val notification = buildNotification(bnpGuardianLink)
      notification.uniqueIdentifier mustEqual bnpGuardianLink.id
      notification.`type` mustEqual NotificationType.BreakingNews
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
      notification.target.regions mustEqual Set(UK, US)

      val androidBody = notification.payloads.android.get.body
      androidBody("topics") mustEqual "breaking//sport,newsstand//newsstandIos"
      androidBody("editions") mustEqual "uk,us"
      notification.importance mustEqual Importance.Major
    }

    "return a Notification if the type is BreakingNewsPayload" in new BreakingNewsScope {
      val notification = buildNotification(bnp)
      notification.`type` mustEqual NotificationType.BreakingNews
    }
  }

  trait PlatFormLinkTestScope extends Scope {
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
      link = GuardianLinkDetails("contentId", Some("/p/4fv33"), "myTitle", None, GITContent),
      importance = Importance.Major,
      topic = Set.empty,
      debug = true
    )

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
      customProperties = Map("t" -> "m", "notificationType" -> "news", "link" -> "x-gu:///p/4fv33", "topics" -> ""),
      category = Some("ITEM_CATEGORY")
    )
  }

  trait BreakingNewsScope extends Scope {

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
  }

  trait ContentAlertScope extends Scope {

    val link = GuardianLinkDetails(contentApiId = "capiId", shortUrl = Some("http://gu.com/short/url"), title = "some title", thumbnail = None, git = GITContent)

    val cap = ContentAlertPayload(
      id = "contentAlertId",
      title = "myTitle",
      message = "myMessage",
      thumbnailUrl = Some(new URI("http://thumb.url.com")),
      sender = "mySender",
      link = link,
      importance = Importance.Minor,
      topic = Set(Topic(TopicTypes.Content, "topicName"), Topic(TopicTypes.Content, "topicName2")),
      debug = true
    )

    val expectedAndroidPayload = AndroidMessagePayload(
      Map(
        "link" -> "x-gu://www.guardian.co.uk/capiId",
        "thumbnailUrl" -> "http://thumb.url.com",
        "message" -> "myMessage",
        "title" -> "myTitle",
        "type" -> "custom",
        "ticker" -> "myMessage",
        "topics" -> "content//topicName,content//topicName2"
      ))

    val expectedIosPayload = IOSMessagePayload(
      body = "myTitle",
      customProperties = Map(
        "t" -> "m",
        "notificationType" -> "content",
        "link" -> "x-gu:///short/url",
        "topics" -> "content//topicName,content//topicName2"),
      category = Some("ITEM_CATEGORY")
    )

    val expectedMetadata = Map(
      "title" -> "myTitle",
      "message" -> "myMessage",
      "link" -> "http://www.theguardian.com/capiId"
    )
  }

}
