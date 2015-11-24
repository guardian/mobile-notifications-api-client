package com.gu.mobile.notifications.client.models

object Importance {
  sealed trait Importance
  object Minor extends Importance
  object Major extends Importance
}