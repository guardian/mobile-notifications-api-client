package com.gu.mobile.notifications.client.legacy

import java.net.URI

import org.specs2.mutable.Specification

object testBuilder extends PlatformPayloadBuilder {
  def getMapiUrl(stringUrl: String) = replaceHost(new URI(stringUrl))
}

class PlatformPayloadBuilderSpec extends Specification {

  "toMapiUrl" should {
    "return correct Url for full addresses" in {
      testBuilder.getMapiUrl("https://mobile.code.dev-guardianapis.com/search?query=sport&page=2") mustEqual ("x-gu://MAPI/search?query=sport&page=2")
    }
    "return correct Url without query string" in {
      testBuilder.getMapiUrl("https://mobile.code.dev-guardianapis.com/navigation/uk") mustEqual ("x-gu://MAPI/navigation/uk")
    }
    "return correct Url without path" in {
      testBuilder.getMapiUrl("https://mobile.code.dev-guardianapis.com?someParam=someValue") mustEqual ("x-gu://MAPI?someParam=someValue")
    }
    "return correct Url without path or query String" in {
      testBuilder.getMapiUrl("https://mobile.code.dev-guardianapis.com") mustEqual ("x-gu://MAPI")
    }
  }
}