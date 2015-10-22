package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.legacy.{IOSMessagePayload, MessagePayloads, Target, Notification}
import org.specs2.mutable.Specification
import org.specs2.time.NoDurationConversions
import dispatch._
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import com.gu.mobile.notifications.client.models._
import com.github.tomakehurst.wiremock.client.WireMock

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ApiClientSpec extends Specification with WireMockHelper with NoDurationConversions {
  val wireMockHost: String = "localhost"
  val wireMockPort: Int = 9595

  "post" should {
    "not mangle unicode" in {

      val fixture = new ApiClient {
        override def http(
          method: String,
          urlString: String,
          contentType: Option[ContentType] = None,
          body: Option[Array[Byte]] = None
        ): Future[HttpResponse] = {
          var request = url(urlString).setMethod(method)
          request = contentType.map { case ContentType(mediaType, charset) => request.setContentType(mediaType, charset) }.getOrElse(request)
          request = body.map(request.setBody).getOrElse(request)

          Http(request).map { response =>
            if (response.getStatusCode / 100 == 2) {
              HttpOk(response.getStatusCode, response.getResponseBody)
            } else {
              HttpError(response.getStatusCode, response.getResponseBody)
            }
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
