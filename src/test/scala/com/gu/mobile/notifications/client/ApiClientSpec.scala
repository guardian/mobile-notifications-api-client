package com.gu.mobile.notifications.client

import org.specs2.mutable.Specification
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.WireMock.{ equalTo => wireMockEqualTo }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.specs2.mutable.BeforeAfter
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import dispatch.Http
import com.gu.mobile.notifications.client.models.Notification
import com.gu.mobile.notifications.client.models.Target
import com.gu.mobile.notifications.client.models.MessagePayloads

class ApiClientSpec extends Specification {

  val Port = 8080
  val HostName = "localhost"
  

  class ApiClientImpl extends ApiClient {
    val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    val host = s"http://$HostName:$Port"
    def httpClient: Http = Http
  }

  val client = new ApiClientImpl

  trait StubServer extends BeforeAfter {
    val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

    def before = {
      wireMockServer.start()
      WireMock.configureFor(HostName, Port)
    }

    def after = wireMockServer.stop()
  }

  val notification = Notification("news", "sender", Target(Set(), Set()), 20, MessagePayloads(None, None), Map())

  "ApiClient" should {
    "post request to notifications service" in new StubServer {
      val path = s"/notifications"
      stubFor(post(urlEqualTo(path)).withRequestBody(
        equalToJson("""
              {"type":"news","sender":"sender","target":{"regions":[],"topics":[]},"timeToLiveInSeconds":20,"payloads":{},"metadata":{}}
              """))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("""{"messageId":"123"}""")))

      val responseFuture = client.send(notification)

      val response = Await.result(responseFuture, Duration(100, TimeUnit.MILLISECONDS))
      response.messageId mustEqual "123"
    }
  }

}