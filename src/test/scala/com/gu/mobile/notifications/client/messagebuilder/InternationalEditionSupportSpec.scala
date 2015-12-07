package com.gu.mobile.notifications.client.messagebuilder


import com.gu.mobile.notifications.client.models.Topic._
import com.gu.mobile.notifications.client.models.{Topic, ExternalLink, Importance, BreakingNewsPayload}
import com.gu.mobile.notifications.client.models.Editions._
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class InternationalEditionSupportSpec extends Specification {

  "International edition support" should {
    val AllEditionsTopics: Set[Topic] = Set(BreakingNewsUk, BreakingNewsUs, BreakingNewsAu)

    "extract editions from message" in {
      "add International edition when message contains all editions" in new internationalEdition {
        val editions = withSupport.editionsFrom(msg.copy(topic = AllEditionsTopics))
        editions must contain(International)
      }

      "return original editions if message contains only UK edition" in new internationalEdition {
        val editions = withSupport.editionsFrom(msg.copy(topic = Set(BreakingNewsUk)))
        editions must beEqualTo(Set(UK))
      }
    }

    "extract regions from message" in {
      "add International region when message contains all editions" in new internationalEdition {
        val editions = withSupport.editionsFrom(msg.copy(topic = AllEditionsTopics))
        editions must contain(International)
      }

      "return original regions if message contains only UK edition" in new internationalEdition {
        val editions = withSupport.editionsFrom(msg.copy(topic = Set(BreakingNewsUk)))
        editions must beEqualTo(Set(UK))
      }
    }
  }

  trait internationalEdition extends Scope {
    val withSupport = new InternationalEditionSupport {}
    val msg = BreakingNewsPayload(
      title = "custom",
      message = "message",
      link = ExternalLink("http://www.theguardian.com/world/2015/oct/30/shaker-aamer-lands-back-in-uk-14-years-in-guantanamo-bay"),
      thumbnailUrl = None,
      imageUrl = None,
      sender = "sender",
      importance = Importance.Major,
      topic = Set.empty,
      debug = true
    )
  }
}