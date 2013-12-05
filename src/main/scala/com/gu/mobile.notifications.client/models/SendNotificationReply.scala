package com.gu.mobile.notifications.client.models

/** Acknowledgement of notification with a message ID for looking up statistics on that message */
case class SendNotificationReply(messageId: String)