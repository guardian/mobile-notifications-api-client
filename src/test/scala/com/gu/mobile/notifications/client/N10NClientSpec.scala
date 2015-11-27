package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.NotificationTypes.BreakingNews
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy._
import org.specs2.execute.Result
import play.api.libs.json.Json

import scala.concurrent.Future


class N10NClientSpec extends ApiClientSpec[N10nApiClient] {

  val payload = BreakingNewsPayload(
    title = "myTitle",
    `type` = BreakingNews.toString,
    message = "myMessage",
    sender = "test sender",
    editions = Set.empty,
    imageUrl = None,
    thumbnailUrl = None,
    link = ExternalLink("http://mylink"),
    importance = Importance.Major,
    topic = Set(Topic("t1", "n1"), Topic("t2", "n2")),
    debug = true
  )

  val expectedPostUrl = s"$host/push/topic/t1/n1?api-key=$apiKey"
  val expectedPostBody = Json.stringify(Json.toJson(payload))

  override def getTestApiClient(httpProvider: HttpProvider) = new N10nApiClient(
    apiKey = apiKey,
    httpProvider = httpProvider,
    host = host
  )
  def apiTest(test: N10nApiClient => Unit): Result = {
    val successServerResponse = HttpOk(200, "")
    apiTest(successServerResponse)(test)
  }

  "n10nApiClient" should {
    "successfully send BreakingNewsPayload" in apiTest {
      n10nClient => n10nClient.send(payload) must beEqualTo(Right()).await
    }
    "return HttpApiError error if http provider returns httpError" in apiTest(serverResponse = HttpError(500, "")) {
      n10nClient => n10nClient.send(payload) must beEqualTo(Left(HttpApiError(status = 500))).await
    }

    "return missing parameter error if payload has no topic" in {
      val payloadWithNoTopics = BreakingNewsPayload(
        title = "myTitle",
        `type` = BreakingNews.toString,
        message = "myMessage",
        sender = "test sender",
        editions = Set.empty,
        imageUrl = None,
        thumbnailUrl = None,
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = Set.empty,
        debug = true
      )

      val n10nClient = getTestApiClient(mock[HttpProvider])
      n10nClient.send(payloadWithNoTopics) must beEqualTo(Left(MissingParameterError("topic"))).await
    }

    "return HttpProviderError if http provider throws exception" in {
      val throwable = new RuntimeException("something went wrong!!")
      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.failed(throwable)
      val n10nClient = getTestApiClient(fakeHttpProvider)
      n10nClient.send(payload) must beEqualTo(Left(HttpProviderError(throwable))).await
    }
  }

}
