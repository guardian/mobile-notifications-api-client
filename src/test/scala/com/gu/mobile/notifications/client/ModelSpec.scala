package com.gu.mobile.notifications.client.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import JsonImplicits._

class ModelSpec extends Specification  {
  "IOSMessagePayload" should {

    "encode the category option when present" in {
      val payload = IOSMessagePayload("body goes here", Map.empty, Some("12CategoryGoesHere34"))
      val payloadJsonStr = Json.stringify(Json.toJson(payload))

      payloadJsonStr must contain("12CategoryGoesHere34")

      1 mustEqual 1
    }
  }

}
