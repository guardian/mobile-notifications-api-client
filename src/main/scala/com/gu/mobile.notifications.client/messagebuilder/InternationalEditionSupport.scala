package com.gu.mobile.notifications.client.messagebuilder

import com.gu.mobile.notifications.client.models.BreakingNewsPayload
import com.gu.mobile.notifications.client.models.Regions._

trait InternationalEditionSupport {
  final val AllEditions: Set[Region] = Set(UK, US, AU)

  def editionsFrom(message: BreakingNewsPayload): Set[String] =
    if (message.editions == AllEditions.map(_.toString))
      message.editions + International.toString
    else
      message.editions

  def regionsFrom(message: BreakingNewsPayload): Set[Region] = {
    val regions: Set[Region] = message.editions.collect {
      case "uk" => UK
      case "us" => US
      case "au" => AU
    }
    if (regions == AllEditions) regions + International else regions
  }
}
