package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.legacy.{IOSMessagePayload, MessagePayloads, Target, Notification}
import org.specs2.mutable.Specification
import org.specs2.time.{NoTimeConversions, NoDurationConversions}
import dispatch._
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import com.gu.mobile.notifications.client.models._
import com.github.tomakehurst.wiremock.client.WireMock

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import java.util.UUID

import com.gu.mobile.notifications.client.ApiClient
import com.gu.mobile.notifications.client.models.Regions.{US, UK}
import com.gu.mobile.notifications.client.models.{SendNotificationReply, ExternalLink, Link}
import models.CustomMessage
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future

class ApiClientSpec extends Specification with NoDurationConversions with WireMockHelper with NoTimeConversions {
  val wireMockHost: String = "localhost"
  val wireMockPort: Int = 9595

  "ApiClient" should {
    "generate the right payload for a platform" in {
      "containing ios payload only if ios is required" in new NotificationScope {
        val notification = serviceApi.buildNotification(message(platforms = Set("ios")), "test", link)
        notification.payloads.android must beNone
        notification.payloads.ios must beSome
      }
      "containing android payload only if android is required" in new NotificationScope {
        val notification = serviceApi.buildNotification(message(platforms = Set("android")), "test", link)
        notification.payloads.android must beSome
        notification.payloads.ios must beNone
      }
      "containing android and ios payloads if both are required" in new NotificationScope {
        val notification = serviceApi.buildNotification(message(platforms = Set("android", "ios")), "test", link)
        notification.payloads.android must beSome
        notification.payloads.ios must beSome
      }
    }
    "generate the right edition for a platform" in {
      "keep all the existing edition in the target" in new NotificationScope {
        val notification = serviceApi.buildNotification(message(editions = Set("uk", "us")), "test", link)
        notification.target.regions mustEqual Set(UK, US)
      }
      "filter non existing editions without crashing" in new NotificationScope {
        val notification = serviceApi.buildNotification(message(editions = Set("uk", "potato")), "test", link)
        notification.target.regions mustEqual Set(UK)
      }
    }
    "post data understood by the service module" in new NotificationScope with MockedHttpClient {
      val notification = serviceApi.buildNotification(message(editions = Set("uk")), "test", link)
      val reply = serviceApi.notificationClient.send(notification)

      reply must beEqualTo(SendNotificationReply("123")).await(timeout = 5 seconds)
      Json.parse(postedBody) mustEqual expectedParsedJson
    }
  }

  trait NotificationScope extends Scope {
    val serviceApi = new ServicesApi
    def message(platforms: Set[String] = Set("ios", "android"), editions: Set[String] = Set("uk")): CustomMessage = CustomMessage(
      title = "Sweden school attack: police treat killing of pupil and teacher as racist hate crime",
      message = "Anton Lundin Pettersson, 21, chose his victims at Kronan school in Trollhättan based on the colour of their skin, say police",
      thumbnail = Some("http://thum.nail/1.jpg"),
      link = "http://li.nk",
      imageUrl = Some("http://image.url"),
      editions = editions,
      platforms = platforms,
      topics = Set()
    )
    val link: Link = ExternalLink("http://www.theguardian.com/world/2015/oct/23/sweden-school-attack-police-investigate-racist-motive-for-double")
  }

  trait MockedHttpClient {
    self: NotificationScope =>
    var postedBody = "{}"

    override val notificationClient: ApiClient = new ApiClient {
      override def apiKey = "myKey"
      override val host = "myHost"
      override def get(url: String): Future[HttpResponse] = Future.successful(HttpError(500, "Not implemented"))
      override def post(url: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] = {
        postedBody = new String(body)
        Future.successful(HttpOk(200, "{\"messageId\":\"123\"}"))
      }

    val expectedJson = s"""{
                          |"type": "news",
                          |"uniqueIdentifier": "${UUID.fromString("ca907fae-9ec3-44f9-bb17-df4d1afe5c22").toString}",
                          |"sender": "test",
                          |"target": {
                          |"regions": [
                          |"uk"
                          |],
                          |"topics": []
                          |},
                          |"timeToLiveInSeconds": 7200,
                          |"payloads": {
                          |"ios": {
                          |"type": "ios",
                          |"body": "Anton Lundin Pettersson, 21, chose his victims at Kronan school in Trollhättan based on the colour of their skin, say police",
                          |"customProperties": {
                          |"t": "m",
                          |"notificationType": "news",
                          |"link": "http://www.theguardian.com/world/2015/oct/23/sweden-school-attack-police-investigate-racist-motive-for-double"
                          |}
                          |},
                          |"android": {
                          |"type": "android",
                          |"body": {
                          |"editions": "uk",
                          |"edition": "uk",
                          |"debug": "true",
                          |"notificationType": "news",
                          |"link": "http://www.theguardian.com/world/2015/oct/23/sweden-school-attack-police-investigate-racist-motive-for-double",
                          |"message": "Anton Lundin Pettersson, 21, chose his victims at Kronan school in Trollhättan based on the colour of their skin, say police",
                          |"title": "Sweden school attack: police treat killing of pupil and teacher as racist hate crime",
                          |"type": "custom",
                          |"ticker": "Anton Lundin Pettersson, 21, chose his victims at Kronan school in Trollhättan based on the colour of their skin, say police",
                          |"imageUrl": "http://image.url",
                          |"thumbnailUrl": "http://thum.nail/1.jpg"
                          |}
                          |}
                          |},
                          |"metadata": {
                          |"title": "Sweden school attack: police treat killing of pupil and teacher as racist hate crime",
                          |"message": "Anton Lundin Pettersson, 21, chose his victims at Kronan school in Trollhättan based on the colour of their skin, say police",
                          |"link": "http://li.nk"
                          |}
                          |}""".stripMargin

    val expectedParsedJson = Json.parse(expectedJson)
  }

  "post" should {
    "not mangle unicode" in {

      val fixture = new ApiClient {
        override def get(url: String): Future[HttpResponse] = Future.successful(HttpError(418, "I'm a teapot"))
        override def post(urlString: String, contentType: ContentType, body: Array[Byte]): Future[HttpResponse] =
          execute(url(urlString)
            .setMethod("POST")
            .setContentType(contentType.mediaType, contentType.charset)
            .setBody(body)
          )

        private def execute(request: Req) = Http(request).map { response =>
          if (response.getStatusCode >= 200 && response.getStatusCode < 300) {
            HttpOk(response.getStatusCode, response.getResponseBody)
          } else {
            HttpError(response.getStatusCode, response.getResponseBody)
          }
        }

        /** Host of the Guardian Notifications Service */
        override def host: String = s"http://$wireMockHost:$wireMockPort"
      }

      StubServer {
        stubFor(
          post(urlEqualTo("/notifications"))
            .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "text/html")
              .withBody(Json.stringify(Json.toJson(SendNotificationReply("messageId"))))))

        val future = fixture.send(Notification(
          BreakingNews,
          "",
          "",
          Target(Set.empty, Set.empty),
          0,
          MessagePayloads(Some(IOSMessagePayload(
            "Masters 2014: round one – live!",
            Map.empty
          )), None),
          Map.empty
        ))

        Await.ready(future, Duration(5, "s"))

        verify(postRequestedFor(urlMatching("/notifications"))
          .withRequestBody(WireMock.containing("Masters 2014: round one – live!"))
        )

        1 mustEqual 1
      }
    }
  }

}
