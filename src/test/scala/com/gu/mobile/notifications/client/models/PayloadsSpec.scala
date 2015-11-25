package com.gu.mobile.notifications.client.models

import java.net.URL

import com.gu.mobile.notifications.client.models.Importance.Importance
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.legacy.Topic
import org.specs2.mutable.Specification
import play.api.libs.json.Json


class PayloadsSpec extends Specification {


  "NotificationPayload" should {
    def verifySerialization(payload: NotificationPayload, expectedJson: String) = Json.stringify(Json.toJson(payload)) shouldEqual (expectedJson)

    "define serializable Breaking News payload" in {

      val payload = BreakingNewsPayload(
        title = "myTitle",
        notificationType = BreakingNews.toString,
        message = "myMessage",
        sender = "test sender",
        editions = Set("ed1", "ed2"),
        imageUrl = Some("http://something.com/img.jpg"),
        thumbnailUrl = Some(new URL("http://something.com/thumb.jpg")),
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = Set(Topic("t1", "n1"), Topic("t2", "n2")),
        debug = true)
      val expectedJson = """{"title":"myTitle","notificationType":"news","message":"myMessage","thumbnailUrl":"http://something.com/thumb.jpg","sender":"test sender","editions":["ed1","ed2"],"link":{"url":"http://mylink"},"imageUrl":"http://something.com/img.jpg","importance":"Major","topic":[{"type":"t1","name":"n1"},{"type":"t2","name":"n2"}],"debug":true}"""

      verifySerialization(payload, expectedJson)
    }

    "define serializable Content Alert payload" in {

      val payload = ContentAlertPayload(
        "myTitle",
        notificationType = "someType",
        message = "someMessage",
        thumbnailUrl = Some(new URL("http://something.com/thumb.jpg")),
        sender = "someSender",
        link = ExternalLink("http://mylink"),
        importance = Importance.Minor,
        topic = Set(Topic("t4", "n4"), Topic("t2", "n2"), Topic("t7", "n7")),
        debug = false,
        shortUrl = "http://a.uk")

      val expectedJson = """{"title":"myTitle","notificationType":"someType","message":"someMessage","thumbnailUrl":"http://something.com/thumb.jpg","sender":"someSender","link":{"url":"http://mylink"},"importance":"Minor","topic":[{"type":"t4","name":"n4"},{"type":"t2","name":"n2"},{"type":"t7","name":"n7"}],"debug":false,"shortUrl":"http://a.uk"}"""

      verifySerialization(payload, expectedJson)
    }

    "define seriazable Goal Alert Payload" in {
      val payload = GoalAlertPayload(
        title = "myTitle",
        notificationType = "someType",
        message = "some Message",
        thumbnailUrl = Some(new URL("http://url.net")),
        sender = "someSender",
        goalType = OwnGoalType, //TODO CHECK DIFFERENT GOAL TYPES
        awayTeamName = "someAwayTeam",
        awayTeamScore = 1,
        homeTeamName = "someHomeTeam",
        homeTeamScore = 2,
        scoringTeamName = "someScoringTeamName",
        scorerName = "someFootballersName",
        goalMins = 41,
        otherTeamName = "someOtherTeamName",
        matchId = "someMatchId",
        mapiUrl = "http://mapi.com/something",
        importance = Importance.Major,
        topic = Set(Topic("t1", "n1"), Topic("tn", "nn")),
        debug = true,
        addedTime = Some("someAddedTime"))

      val expectedJson = """{"title":"myTitle","notificationType":"someType","message":"some Message","thumbnailUrl":"http://url.net","sender":"someSender","goalType":"Own","awayTeamName":"someAwayTeam","awayTeamScore":1,"homeTeamName":"someHomeTeam","homeTeamScore":2,"scoringTeamName":"someScoringTeamName","scorerName":"someFootballersName","goalMins":41,"otherTeamName":"someOtherTeamName","matchId":"someMatchId","mapiUrl":"http://mapi.com/something","importance":"Major","topic":[{"type":"t1","name":"n1"},{"type":"tn","name":"nn"}],"debug":true,"addedTime":"someAddedTime"}"""

      verifySerialization(payload, expectedJson)

    }
    "define seriazable goal types" in {
      def verifySerialization(gType: GoalType, expectedJson: String) = Json.stringify(Json.toJson(gType)) shouldEqual (expectedJson)
      verifySerialization(OwnGoalType, "\"Own\"")
      verifySerialization(PenaltyGoalType, "\"Penalty\"")
      verifySerialization(DefaultGoalType, "\"Default\"")
    }
    "define serializable guardian link details" in {
      def verifySerialization(link: GuardianLinkDetails, expectedJson: String) = Json.stringify(Json.toJson(link)) shouldEqual (expectedJson)

      verifySerialization(
        link = GuardianLinkDetails("cApiId", Some("url"), "someTitle", Some("thumb"), GITSection),
        expectedJson = """{"contentApiId":"cApiId","shortUrl":"url","title":"someTitle","thumbnail":"thumb","git":{"mobileAggregatorPrefix":"section"}}"""
      )

      verifySerialization(
        link = GuardianLinkDetails("cApiId", Some("url"), "someOtherTitle", Some("thumb"), GITTag),
        expectedJson = """{"contentApiId":"cApiId","shortUrl":"url","title":"someOtherTitle","thumbnail":"thumb","git":{"mobileAggregatorPrefix":"latest"}}"""
      )

      verifySerialization(
        link = GuardianLinkDetails("cApiId", Some("url"), "someTitle", Some("thumb"), GITContent),
        expectedJson = """{"contentApiId":"cApiId","shortUrl":"url","title":"someTitle","thumbnail":"thumb","git":{"mobileAggregatorPrefix":"item-trimmed"}}"""
      )

    }
  }
}
