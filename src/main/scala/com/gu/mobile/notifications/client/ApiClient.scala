package com.gu.mobile.notifications.client

import scala.concurrent.{ExecutionContext, Future}

import com.gu.mobile.notifications.client.models.{Notification, SendNotificationReply}

import dispatch._
import grizzled.slf4j.Logging
import net.liftweb.json.{NoTypeHints, Serialization}
import net.liftweb.json.Serialization.{read, write}

trait ApiClient extends Logging {
  implicit val executionContext: ExecutionContext
  implicit val formats = Serialization.formats(NoTypeHints)

  /** Host of the Guardian Notifications Service */
  def host: String

  /** Http client */
  def httpClient: Http

  def send(notification: Notification): Future[SendNotificationReply] = 
    send(write(notification))
  
  private def send(json: String): Future[SendNotificationReply] = {
    debug(s"request body: $json")
    httpClient((url(host) / "notifications") << json OK as.String) map { body =>
      read[SendNotificationReply](body)
    }
  }
}
