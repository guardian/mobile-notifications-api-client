package com.gu.mobile.notifications.client.models

object Priority {
  sealed trait Priority
  object Minor extends Priority
  object Major extends Priority
}