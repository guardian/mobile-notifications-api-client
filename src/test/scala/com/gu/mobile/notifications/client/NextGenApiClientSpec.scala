package com.gu.mobile.notifications.client

import com.gu.mobile.notifications.client.models.TopicTypes._
import com.gu.mobile.notifications.client.models._
import org.specs2.execute.Result
import play.api.libs.json.Json

import scala.concurrent.Future


class NextGenApiClientSpec extends ApiClientSpec[NextGenApiClient] {

  val payload = BreakingNewsPayload(
    title = "myTitle",
    message = "myMessage",
    sender = "test sender",
    imageUrl = None,
    thumbnailUrl = None,
    link = ExternalLink("http://mylink"),
    importance = Importance.Major,
    topic = Set(Topic(Breaking, "n1"), Topic(FootballMatch, "n2")),
    debug = true
  )

  val expectedPostUrl = s"$host/push/topic/$Breaking/n1?api-key=$apiKey"
  val expectedPostBody = Json.stringify(Json.toJson(payload))

  override def getTestApiClient(httpProvider: HttpProvider) = new NextGenApiClient(
    apiKey = apiKey,
    httpProvider = httpProvider,
    host = host
  )
  def apiTest(test: NextGenApiClient => Unit): Result = {
    val successServerResponse = HttpOk(201, """{"id":"someId"}""")
    apiTest(successServerResponse)(test)
  }

  "NextGenApiClient" should {
    "successfully send payload" in apiTest {
      client => client.send(payload) must beRight.await
    }
    "return HttpApiError error if http provider returns ApiHttpError" in apiTest(serverResponse = HttpError(500, "")) {
      client => client.send(payload) must beEqualTo(Left(ApiHttpError(status = 500))).await
    }
    "return UnexpectedApiResponseError if server returns invalid json" in apiTest(serverResponse = HttpOk(201, "not valid json at all")) {
      client => client.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("not valid json at all"))).await
    }
    "return UnexpectedApiResponseError if server returns wrong json format" in apiTest(serverResponse = HttpOk(201, """{"unexpected":"yes"}""")) {
      client => client.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("""{"unexpected":"yes"}"""))).await
    }
    "return UnexpectedApiResponseError if server returns wrong success status code" in apiTest(serverResponse = HttpOk(200, "success but not code 201!")) {
      client => client.send(payload) must beEqualTo(Left(UnexpectedApiResponseError("Server returned status code 200 and body:success but not code 201!"))).await
    }

    "return missing parameter error if payload has no topic" in {
      val payloadWithNoTopics = BreakingNewsPayload(
        title = "myTitle",
        message = "myMessage",
        sender = "test sender",
        imageUrl = None,
        thumbnailUrl = None,
        link = ExternalLink("http://mylink"),
        importance = Importance.Major,
        topic = Set.empty,
        debug = true
      )

      val client = getTestApiClient(mock[HttpProvider])
      client.send(payloadWithNoTopics) must beEqualTo(Left(MissingParameterError("topic"))).await
    }

    "return HttpProviderError if http provider throws exception" in {
      val throwable = new RuntimeException("something went wrong!!")
      val fakeHttpProvider = mock[HttpProvider]
      fakeHttpProvider.post(anyString, any[ContentType], any[Array[Byte]]) returns Future.failed(throwable)
      val client = getTestApiClient(fakeHttpProvider)
      client.send(payload) must beEqualTo(Left(HttpProviderError(throwable))).await
    }
  }

}
