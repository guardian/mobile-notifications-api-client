package com.gu.mobile.notifications.client.models

import org.specs2.mutable.Specification

class LinkTest extends Specification {

  "toDeepLink" should {
    "return the url if the Link is of type ExternalLink" in {
      val link = ExternalLink("myLink")
      link.toDeepLink mustEqual "myLink"
    }

    "return the correct deep link if the Link is of type GuardianLinkDetails" in {
      val someLink = GuardianLinkDetails("myLink1", Some("https://shortUrl"),"myTitle", None, GITContent)
      val noneLink = GuardianLinkDetails("myLink2", None, "myTitle", None, GITContent)
      someLink.toDeepLink mustEqual "x-gu://shortUrl"
      noneLink.toDeepLink mustEqual noneLink.webUrl.replace("http", "x-gu")
    }

  }

  "toString" should {
    "return the url if the Link is of type ExternalLink" in {
      val link = ExternalLink("myLink")
      link.toString mustEqual "myLink"
    }

    "return the correct link if the Link is of type GuardianLinkDetails" in {
      val link = GuardianLinkDetails("myLink1", Some("http://shortUrl"),"myTitle", None, GITContent)
      link.toString mustEqual link.webUrl
    }
  }

}
