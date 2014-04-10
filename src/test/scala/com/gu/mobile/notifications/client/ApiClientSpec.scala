package com.gu.mobile.notifications.client

import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import dispatch.Http
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import com.gu.mobile.notifications.client.models._
import com.github.tomakehurst.wiremock.client.WireMock
import JsonImplicits._

class ApiClientSpec extends Specification with WireMockHelper {
  val wireMockHost: String = "localhost"
  val wireMockPort: Int = 9595

  "post" should {
    "not mangle unicode" in {

      val fixture = new ApiClient {
        /** Http client */
        override def httpClient: Http = Http

        /** Host of the Guardian Notifications Service */
        override def host: String = s"http://$wireMockHost:$wireMockPort"

        override implicit val executionContext = global
      }

      StubServer {
        stubFor(
          post(urlEqualTo("/notifications"))
            .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/html")
            .withBody(Json.stringify(Json.toJson(SendNotificationReply("messageId"))))))

        fixture.send(Notification(
          "",
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

        verify(postRequestedFor(urlMatching("/notifications"))
          .withRequestBody(WireMock.matching(".*Masters 2014: round one – live!.*"))
        )

        1 mustEqual 1
      }
    }
  }
}
