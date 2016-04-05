package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.legacy.NotificationBuilderImpl._
import com.gu.mobile.notifications.client.models.Editions._
import com.gu.mobile.notifications.client.models.Importance.{Major, Minor}
import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
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
    val goalAlertTitle = "goal alert title"
    val goalAlertSender = "goalAlertSender"
    val goalAlertMessage = "goal alert Message"
    val link = GuardianLinkDetails(contentApiId = "capiId", shortUrl = Some("http://gu.com/short/url"), title = "some title", thumbnail = None, git = GITContent)
    val homeTeamName = "home"
    val awayTeamName = "away"
    val homeTeamId = "homeTeamId"
    val awayTeamId = "awayTeamId"
    val matchId = "matchId"
    val scoringTeamName = "scoringTeam"
    val otherTeamName = "otherTeam"
    val scorerName = "scorerName"
    val minutes = 91
    val awayScore = 7
    val homeScore = 6
    val goalMins = 91
    val mapiURl = "http://mapi.url.com"

    val topics = Set(
      Topic(
        TopicTypes.FootballTeam,
        homeTeamId
      ),
      Topic(
        TopicTypes.FootballTeam,
        awayTeamId
      ),
      Topic(
        TopicTypes.FootballMatch,
        matchId
      ),
      Topic(
        TopicTypes.FootballTeam,
        homeTeamName
      ),
      Topic(
        TopicTypes.FootballTeam,
        awayTeamName
      )
    )
    val gap = GoalAlertPayload(
      id = "ID",
      title = goalAlertTitle,
      message = goalAlertMessage,
      thumbnailUrl = None,
      sender = goalAlertSender,
      goalType = OwnGoalType,
      awayTeamName = awayTeamName,
      awayTeamScore = awayScore,
      homeTeamName = homeTeamName,
      homeTeamScore = homeScore,
      scoringTeamName = scoringTeamName,
      scorerName = scorerName,
      goalMins = goalMins,
      otherTeamName = otherTeamName,
      matchId = matchId,
      mapiUrl = new URI(mapiURl),
      importance = Major,
      topic = topics,
      debug = true,
      addedTime = Some("addedTime")
    )

    val expectedAndroidPayload = AndroidMessagePayload(
      Map(
        "type" -> "goalAlert",
        "AWAY_TEAM_NAME" -> awayTeamName,
        "AWAY_TEAM_SCORE" -> awayScore.toString,
        "HOME_TEAM_NAME" -> homeTeamName,
        "HOME_TEAM_SCORE" -> homeScore.toString,
        "SCORER_NAME" -> scorerName,
        "GOAL_MINS" -> goalMins.toString,
        "OTHER_TEAM_NAME" -> otherTeamName,
        "SCORING_TEAM_NAME" -> scoringTeamName,
        "matchId" -> "matchId",
        "mapiUrl" -> mapiURl,
        "debug" -> "true"
      ))

    val expectedIosPayload = IOSMessagePayload(
      body = goalAlertMessage,
      customProperties = Map("t" -> "g")
    )

    val expectedMetadata = Map(
      "matchId" -> matchId,
      "homeTeamName" -> homeTeamName,
      "homeTeamScore" -> homeScore.toString,
      "awayTeamName" -> awayTeamName,
      "awayTeamScore" -> awayScore.toString,
      "scorer" -> scorerName,
      "minute" -> goalMins.toString
    )

    val expectedNotification = Notification(
      uniqueIdentifier = "goalAlert/matchId/6-7/91",
      timeToLiveInSeconds = 3540,
      `type` = NotificationType.GoalAlert,
      sender = goalAlertSender,
      target = Target(Set.empty, topics),
      payloads = MessagePayloads(Some(expectedIosPayload), Some(expectedAndroidPayload)),
      metadata = expectedMetadata,
      importance = Major
    )
  }

}
