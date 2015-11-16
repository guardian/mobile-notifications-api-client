package com.gu.mobile.notifications.client.lib

import java.util.{UUID => JUUID}

object UUID {
  def next = JUUID.randomUUID.toString
}
