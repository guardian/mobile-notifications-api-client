package com.gu.mobile.notifications.client.models

import java.net.URL

import com.gu.mobile.notifications.client.models.legacy.Topic
import org.specs2.mutable.Specification
import play.api.libs.json.Json


class PayloadsSpec extends Specification {


  "NotificationPayload" should {
    def verifySerialization(payload: NotificationPayload, expectedJson: String) = Json.toJson(payload) shouldEqual Json.parse(expectedJson)

    "define serializable Breaking News payload" in {

      val payload = BreakingNewsPayload(
        id = "30aac5f5-34bb-4a88-8b69-97f995a4907b",
        title = "The Guardian",
        message = "Mali hotel attack: UN counts 27 bodies as hostage situation ends",
        sender = "test",
        editions = Set("ed1", "ed2"),
        imageUrl = Some("https://mobile.guardianapis.com/img/media/a5fb401022d09b2f624a0cc0484c563fd1b6ad93/0_308_4607_2764/master/4607.jpg/6ad3110822bdb2d1d7e8034bcef5dccf?width=800&height=-&quality=85"),
        thumbnailUrl = Some(new URL("http://media.guim.co.uk/09951387fda453719fe1fee3e5dcea4efa05e4fa/0_181_3596_2160/140.jpg")),
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = Set(Topic("breaking", "uk")),
        debug = true)
      val expectedJson =
        """
          |{
          |  "id" : "30aac5f5-34bb-4a88-8b69-97f995a4907b",
          |  "title" : "The Guardian",
          |  "type" : "news",
          |  "message" : "Mali hotel attack: UN counts 27 bodies as hostage situation ends",
          |  "thumbnailUrl" : "http://media.guim.co.uk/09951387fda453719fe1fee3e5dcea4efa05e4fa/0_181_3596_2160/140.jpg",
          |  "sender": "test",
          |  "editions":["ed1","ed2"],
          |  "link" : {
          |    "url": "http://mylink"
          |  },
          |  "imageUrl" : "https://mobile.guardianapis.com/img/media/a5fb401022d09b2f624a0cc0484c563fd1b6ad93/0_308_4607_2764/master/4607.jpg/6ad3110822bdb2d1d7e8034bcef5dccf?width=800&height=-&quality=85",
          |  "importance" : "Major",
          |  "topic" : [ {
          |    "type" : "breaking",
          |    "name" : "uk"
          |  } ],
          |  "debug":true
          |}
        """.stripMargin

      verifySerialization(payload, expectedJson)
    }

    "define serializable Content Alert payload" in {
      val internalLink = GuardianLinkDetails(
        contentApiId = "environment/ng-interactive/2015/oct/16/which-countries-are-doing-the-most-to-stop-dangerous-global-warming",
        shortUrl = Some("http:short.com"),
        title = "linkTitle",
        thumbnail = Some("http://thumb.om"),
        git = GITContent)

      val payload = ContentAlertPayload(
        id = "c8bd6aaa-072f-4593-a38b-322f3ecd6bd3",
        title = "Follow",
        message = "Which countries are doing the most to stop dangerous global warming?",
        thumbnailUrl = Some(new URL("http://media.guim.co.uk/a07334e4ed5d13d3ecf4c1ac21145f7f4a099f18/127_0_3372_2023/140.jpg")),
        sender = "test",
        link = internalLink,
        importance = Importance.Minor,
        topic = Set(Topic("tag-series", "environment/series/keep-it-in-the-ground"), Topic("t2", "n2")),
        debug = false,
        shortUrl = "shortUrl")

      val expectedJson =
        """
          |{
          |  "id" : "c8bd6aaa-072f-4593-a38b-322f3ecd6bd3",
          |  "title" : "Follow",
          |  "type" : "content",
          |  "message" : "Which countries are doing the most to stop dangerous global warming?",
          |  "thumbnailUrl" : "http://media.guim.co.uk/a07334e4ed5d13d3ecf4c1ac21145f7f4a099f18/127_0_3372_2023/140.jpg",
          |  "sender" : "test",
          |  "link" : {
          |    "contentApiId" : "environment/ng-interactive/2015/oct/16/which-countries-are-doing-the-most-to-stop-dangerous-global-warming",
          |    "shortUrl":"http:short.com",
          |    "title":"linkTitle",
          |    "thumbnail":"http://thumb.om",
          |    "git":{"mobileAggregatorPrefix":"item-trimmed"}
          |  },
          |  "importance" : "Minor",
          |  "topic" : [ {
          |    "type" : "tag-series",
          |    "name" : "environment/series/keep-it-in-the-ground"
          |  },{
          |    "type" : "t2",
          |    "name" : "n2"
          |    }],
          |    "debug" : false,
          |  "shortUrl" : "shortUrl"
          |}
        """.stripMargin
      verifySerialization(payload, expectedJson)
    }

    "define seriazable Goal Alert Payload" in {
      val payload = GoalAlertPayload(
        id = "3e0bc788-a27c-4864-bb71-77a80aadcce4",
        title = "The Guardian",
        message = "Leicester 2-1 Watford\nDeeney 75min",
        thumbnailUrl = Some(new URL("http://url.net")),
        sender = "someSender",
        goalType = PenaltyGoalType,
        awayTeamName = "someAwayTeam",
        awayTeamScore = 1,
        homeTeamName = "someHomeTeam",
        homeTeamScore = 2,
        scoringTeamName = "someScoringTeamName",
        scorerName = "someFootballersName",
        goalMins = 41,
        otherTeamName = "someOtherTeamName",
        matchId = "3833380",
        mapiUrl = "http://football.mobile-apps.guardianapis.com/match-info/3833380",
        importance = Importance.Major,
        topic = Set(
          Topic("football-team", "29"),
          Topic("football-team", "41"),
          Topic("football-match", "3833380")
        ),
        debug = true,
        addedTime = Some("someAddedTime"))

      val expectedJson =
       """
         |{
         |  "id" : "3e0bc788-a27c-4864-bb71-77a80aadcce4",
         |  "title" : "The Guardian",
         |  "type" : "goalAlert",
         |  "message" : "Leicester 2-1 Watford\nDeeney 75min",
         |  "thumbnailUrl":"http://url.net",
         |  "sender" : "someSender",
         |  "goalType" : "Penalty",
         |  "awayTeamName" : "someAwayTeam",
         |  "awayTeamScore" : 1,
         |  "homeTeamName" : "someHomeTeam",
         |  "homeTeamScore" : 2,
         |  "scoringTeamName" : "someScoringTeamName",
         |  "scorerName" : "someFootballersName",
         |  "goalMins" : 41,
         |  "otherTeamName" : "someOtherTeamName",
         |  "matchId" : "3833380",
         |  "mapiUrl" : "http://football.mobile-apps.guardianapis.com/match-info/3833380",
         |  "importance" : "Major",
         |  "topic" : [ {
         |    "type" : "football-team",
         |    "name" : "29"
         |  }, {
         |    "type" : "football-team",
         |    "name" : "41"
         |  }, {
         |    "type" : "football-match",
         |    "name" : "3833380"
         |  } ],
         |  "debug":true,
         |  "addedTime":"someAddedTime"
         |}
       """.stripMargin
      verifySerialization(payload, expectedJson)

    }
    "define seriazable goal types" in {
      def verifySerialization(gType: GoalType, expectedJson: String) = Json.toJson(gType) shouldEqual Json.parse(expectedJson)
      verifySerialization(OwnGoalType, "\"Own\"")
      verifySerialization(PenaltyGoalType, "\"Penalty\"")
      verifySerialization(DefaultGoalType, "\"Default\"")
    }
    "define serializable guardian link details" in {
      def verifySerialization(link: GuardianLinkDetails, expectedJson: String) = Json.toJson(link) shouldEqual Json.parse(expectedJson)

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