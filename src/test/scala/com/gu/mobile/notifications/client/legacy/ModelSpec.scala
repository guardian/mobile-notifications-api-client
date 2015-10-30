package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.legacy.IOSMessagePayload
import org.specs2.mutable.Specification
import play.api.libs.json.Json

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
