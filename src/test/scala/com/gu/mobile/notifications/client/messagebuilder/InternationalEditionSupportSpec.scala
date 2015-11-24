package com.gu.mobile.notifications.client.messagebuilder

import com.gu.mobile.notifications.client.models.{ExternalLink, Importance, BreakingNewsPayload}
import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models.Regions.{Region, UK, US, AU, International}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class InternationalEditionSupportSpec extends Specification {
  "International edition support" should {
    val AllEditions: Set[Region] = Set(UK, US, AU)
    val AllEditionsAsString = AllEditions.map(_.toString)

    "extract editions from message" in {
      "add International edition when message contains all editions" in new internationalEdition {
        val editions = withSupport.editionsFrom(msg.copy(editions = AllEditionsAsString))

        editions must contain(International.toString)
      }

      "return original editions if message contains only UK edition" in new internationalEdition {
        val ukOnly = Set("uk")
        val editions = withSupport.editionsFrom(msg.copy(editions = ukOnly))

        editions must beEqualTo(ukOnly)
      }
    }

    "extract regions from message" in {
      "add International region when message contains all editions" in new internationalEdition {
        val regions = withSupport.regionsFrom(msg.copy(editions = AllEditionsAsString))

        regions must contain(International)
      }

      "return original regions if message contains only UK edition" in new internationalEdition {
        val ukOnly = Set("uk")
        val regions = withSupport.regionsFrom(msg.copy(editions = ukOnly))

        regions must beEqualTo(Set(UK))
      }
    }
  }

  trait internationalEdition extends Scope {
    val withSupport = new InternationalEditionSupport {}
    val msg = BreakingNewsPayload(
      title = "custom",
      message = "message",
      notificationType = BreakingNews.toString,
      link = ExternalLink("http://www.theguardian.com/world/2015/oct/30/shaker-aamer-lands-back-in-uk-14-years-in-guantanamo-bay"),
      thumbnailUrl = None,
      imageUrl = None,
      editions = Set.empty,
      sender = "sender",
      priority = Importance.Major,
      topic = Set.empty,
      debug = true
    )
  }
}