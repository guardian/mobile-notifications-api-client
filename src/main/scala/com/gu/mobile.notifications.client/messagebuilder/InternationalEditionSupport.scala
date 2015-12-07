package com.gu.mobile.notifications.client.messagebuilder

import com.gu.mobile.notifications.client.models.TopicTypes.Breaking
import com.gu.mobile.notifications.client.models.{ BreakingNewsPayload}
import com.gu.mobile.notifications.client.models.Editions._

trait InternationalEditionSupport {
  final val AllEditions: Set[Edition] = Set(UK, US, AU)

  def editionsFrom(message: BreakingNewsPayload): Set[Edition] = {
    val breakingNewsTopics = message.topic.filter(_.`type` == Breaking)
      val editions: Set[Edition] = breakingNewsTopics flatMap { _.name match {
        case UK.toString => Some(UK)
        case US.toString => Some(US)
        case AU.toString => Some(AU)
        case International.toString => Some(International)
        case _ => None
      }
    }
    if (editions == AllEditions) editions + International else editions
  }

}
