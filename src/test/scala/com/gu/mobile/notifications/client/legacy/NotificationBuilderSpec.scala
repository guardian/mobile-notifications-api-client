package com.gu.mobile.notifications.client.legacy

import java.net.URI
import java.util.UUID

import com.gu.mobile.notifications.client.legacy.NotificationBuilderImpl._
import com.gu.mobile.notifications.client.models.Editions._
import com.gu.mobile.notifications.client.models.Importance.{Major, Minor}
import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models.legacy._
import com.gu.mobile.notifications.client.models._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class NotificationBuilderSpec extends Specification with Mockito {

  "buildNotification" should {
    "return a well constructed Notification if a valid ContentAlertPayload is provided" in new ContentAlertScope {
      buildNotification(cap) mustEqual expectedNotification
    }

    "return a notification with an imageURI if it is on the ContentAlertPayload" in new ContentAlertScope {
      val contentAlertPayload = cap.copy(imageUrl = Some(new URI("http://big-image.url.com")))
      val expectedAndroidPayloadWithImage = expectedAndroidPayload.copy(body = expectedAndroidPayload.body ++ Map("imageUrl" -> "http://big-image.url.com"))

      val notification = buildNotification(contentAlertPayload)

      notification.payloads.android.get mustEqual expectedAndroidPayloadWithImage
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

    "return a well constructed Notification if a valid goal alert is provided" in new GoalAlertScope {
      buildNotification(gap) mustEqual expectedNotification
    }

    "return a well constructed Notification if a valid breaking news payload is provided" in new BreakingNewsScope {
      buildNotification(bnp) mustEqual expectedNotification
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
        importance = Major,
        topic = Set(BreakingNewsUk, BreakingNewsUs, BreakingNewsSport, NewsstandIos),
        debug = true
      )

      val notification = buildNotification(breakingNewsWithTopics)
      notification.target.topics mustEqual Set(BreakingNewsSport, NewsstandIos)
      notification.target.regions mustEqual Set(UK, US)

      val androidBody = notification.payloads.android.get.body
      androidBody("topics") mustEqual "breaking//sport,newsstand//newsstandIos"
      androidBody("editions") mustEqual "uk,us"
      notification.importance mustEqual Major
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
      importance = Major,
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
      importance = Major,
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
      importance = Major,
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

    val expectedNotification = Notification(
      uniqueIdentifier = "someId",
      `type` = NotificationType.BreakingNews,
      sender = "test sender",
      target = Target(Set.empty, Set.empty),
      payloads = MessagePayloads(Some(expectedIosPayload), Some(expectedAndroidPayload)),
      metadata = Map(
        "title" -> "myTitle",
        "message" -> "myMessage",
        "link" -> "http://mylink"
      ),
      importance = Major
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
      importance = Minor,
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

    val expectedNotification = Notification(
      uniqueIdentifier = "contentNotifications/newArticle/capiId",
      `type` = NotificationType.Content,
      sender = "mySender",
      target = Target(Set.empty, cap.topic),
      payloads = MessagePayloads(Some(expectedIosPayload), Some(expectedAndroidPayload)),
      metadata = expectedMetadata,
      importance = Minor
    )
  }

  trait GoalAlertScope extends Scope {

    val link = GuardianLinkDetails(contentApiId = "capiId", shortUrl = Some("http://gu.com/short/url"), title = "some title", thumbnail = None, git = GITContent)

    val gap = GoalAlertPayload(
      id = "ID",
      title = "goal alert title",
      message = "goal alert message",
      thumbnailUrl = None,
      sender = "goalAlertSender",
      goalType = OwnGoalType,
      awayTeamName = "away",
      awayTeamScore = 7,
      homeTeamName = "home",
      homeTeamScore = 6,
      scoringTeamName = "scoringTeam",
      scorerName = "scorerName",
      goalMins = 91,
      otherTeamName = "otherTeam",
      matchId = "matchId",
      mapiUrl = new URI("http://mapi.url.com"),
      importance = Major,
      topic = Set.empty, // TODO topics?
      debug = true,
      addedTime = Some("addedTime") //TODO why is this a String?
    )

    val expectedAndroidPayload = AndroidMessagePayload(
      Map(
        "type" -> "goalAlert",
        "AWAY_TEAM_NAME" -> "away",
        "AWAY_TEAM_SCORE" -> 7.toString,
        "HOME_TEAM_NAME" -> "home",
        "HOME_TEAM_SCORE" -> 6.toString,
        "SCORER_NAME" -> "scorerName",
        "GOAL_MINS" -> 91.toString,
        "OTHER_TEAM_NAME" -> "otherTeam",
        "SCORING_TEAM_NAME" -> "scoringTeam",
        "matchId" -> "matchId",
        "mapiUrl" -> "http://mapi.url.com",
        "debug" -> "true"
      ))

    val expectedIosPayload = IOSMessagePayload(
      body = "goal alert message",
      customProperties = Map("t" -> "g")
    )

    val expectedMetadata = Map(
      "title" -> "goal alert title",
      "message" -> "goal alert message",
      "link" -> "http://mapi.url.com"
    )

    val expectedNotification = Notification(
      uniqueIdentifier = "ID",
      `type` = NotificationType.GoalAlert,
      sender = "goalAlertSender",
      target = Target(Set.empty, Set.empty),
      payloads = MessagePayloads(Some(expectedIosPayload), Some(expectedAndroidPayload)),
      metadata = expectedMetadata,
      importance = Major
    )
  }

}
