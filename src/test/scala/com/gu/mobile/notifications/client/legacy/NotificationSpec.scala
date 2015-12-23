package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.Editions.{UK, US}
import com.gu.mobile.notifications.client.models.{TopicTypes, Topic}
import com.gu.mobile.notifications.client.models.legacy.NotificationType
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class NotificationSpec extends Specification {
  def verifySerialization(notification: Notification, expectedJson: String) = Json.toJson(notification) shouldEqual Json.parse(expectedJson)

  "Notification " should {
    "generate correct Breaking News json" in {

      val androidPayload = AndroidMessagePayload(
        Map(
          "topics" -> "someTopics",
          "editions" -> "someEditions",
          "debug" -> "true",
          "notificationType" -> "news",
          "link" -> "http://mylink",
          "message" -> "myMessage",
          "title" -> "myTitle",
          "type" -> "custom",
          "ticker" -> "myMessage"
        )
      )

      val iosPayload = IOSMessagePayload(
        body = "myMessage",
        customProperties = Map("t" -> "m", "notificationType" -> "news", "link" -> "http://mylink", "topics" -> "someTopics"),
        category = None
      )
      val metadata = Map("title" -> "someTitle",
        "message" -> "some message",
        "link" -> "http://somelink.com/something")

      val notification = Notification(
        `type` = NotificationType.BreakingNews,
        timeToLiveInSeconds = 10,
        uniqueIdentifier = "someID",
        sender = "some sender",
        target = Target(Set(UK, US), Set(Topic.BreakingNewsUk)),
        payloads = MessagePayloads(Some(iosPayload), Some(androidPayload)),
        metadata = metadata
      )

      val expectedJson =
        """
          |{
          |   "type":"news",
          |   "uniqueIdentifier":"someID",
          |   "sender":"some sender",
          |   "target":{
          |      "regions":[
          |         "uk",
          |         "us"
          |      ],
          |      "topics":[
          |         {
          |            "type":"breaking",
          |            "name":"uk"
          |         }
          |      ]
          |   },
          |   "timeToLiveInSeconds":10,
          |   "payloads":{
          |      "ios":{
          |         "type":"ios",
          |         "body":"myMessage",
          |         "customProperties":{
          |            "t":"m",
          |            "notificationType":"news",
          |            "link":"http://mylink",
          |            "topics":"someTopics"
          |         }
          |      },
          |      "android":{
          |         "type":"android",
          |         "body":{
          |            "topics":"someTopics",
          |            "editions":"someEditions",
          |            "debug":"true",
          |            "notificationType":"news",
          |            "link":"http://mylink",
          |            "message":"myMessage",
          |            "title":"myTitle",
          |            "type":"custom",
          |            "ticker":"myMessage"
          |         }
          |      }
          |   },
          |   "metadata":{
          |      "title":"someTitle",
          |      "message":"some message",
          |      "link":"http://somelink.com/something"
          |   }
          |}
        """.stripMargin

      verifySerialization(notification, expectedJson)

    }
    "generate correct content alert json" in {
      
      val androidPayload = AndroidMessagePayload(
        Map(
          "title" -> "Rugby World Cup: webTitle",
          "link" -> "/newId",
          "type" -> "custom",
          "ticker" -> "webTitle",
          "message" -> "webTitle")
      )

      val iosPayload = IOSMessagePayload(
        body = "Rugby World Cup: webTitle",
        customProperties = Map("t" -> "m", "link" -> "webUrl"),
        category = Some("ITEM_CATEGORY")
      )
      val metadata = Map(
        "title" -> "Following: webTitle",
        "message" -> "webTitle",
        "link" -> "webUrl")

      val notification = Notification(
        `type` = NotificationType.Content,
        timeToLiveInSeconds = 10,
        uniqueIdentifier = "contentNotifications/newArticle/newId",
        sender = "mobile-notifications-content",
        target = Target(Set.empty, Set(Topic(TopicTypes.TagContributor, "tagId1"), Topic(TopicTypes.TagKeyword, "tagId2"), Topic(TopicTypes.TagSeries, "tagId3"))),
        payloads = MessagePayloads(Some(iosPayload), Some(androidPayload)),
        metadata = metadata
      )
      val expectedJson =
        """{
          |   "type":"tag",
          |   "uniqueIdentifier":"contentNotifications/newArticle/newId",
          |   "sender":"mobile-notifications-content",
          |   "target":{
          |      "regions":[
          |
          |      ],
          |      "topics":[
          |         {
          |            "type":"tag-contributor",
          |            "name":"tagId1"
          |         },
          |         {
          |            "type":"tag-keyword",
          |            "name":"tagId2"
          |         },
          |         {
          |            "type":"tag-series",
          |            "name":"tagId3"
          |         }
          |      ]
          |   },
          |   "timeToLiveInSeconds":10,
          |   "payloads":{
          |      "ios":{
          |         "type":"ios",
          |         "body":"Rugby World Cup: webTitle",
          |         "customProperties":{
          |            "t":"m",
          |            "link":"webUrl"
          |         },
          |         "category":"ITEM_CATEGORY"
          |      },
          |      "android":{
          |         "type":"android",
          |         "body":{
          |            "link":"/newId",
          |            "message":"webTitle",
          |            "title":"Rugby World Cup: webTitle",
          |            "type":"custom",
          |            "ticker":"webTitle"
          |         }
          |      }
          |   },
          |   "metadata":{
          |      "title":"Following: webTitle",
          |      "message":"webTitle",
          |      "link":"webUrl"
          |   }
          |}
        """.stripMargin
      verifySerialization(notification, expectedJson)
    }
  }
}
