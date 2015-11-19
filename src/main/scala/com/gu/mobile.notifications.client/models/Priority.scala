package com.gu.mobile.notifications.client.models

sealed trait Priority
object Minor extends Priority
object Major extends Priority
