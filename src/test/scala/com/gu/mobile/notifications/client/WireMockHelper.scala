package com.gu.mobile.notifications.client

import org.specs2.mutable.Specification
import org.specs2.mutable.Around
import org.specs2.execute.AsResult
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.client.WireMock

trait WireMockHelper { self: Specification =>
  val wireMockPort: Int
  val wireMockHost: String

  object StubServer extends Around {
    def around[T: AsResult](t: => T) = {
      val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))
      wireMockServer.start()
      WireMock.configureFor(wireMockHost, wireMockPort)
      val result = AsResult(t)
      wireMockServer.stop()
      result
    }
  }
}
