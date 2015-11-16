package com.gu.mobile.notifications.client.constants

sealed trait Platforms
case object Ios extends Platforms
case object Android extends Platforms
case object Windows extends Platforms
case object Web extends Platforms
