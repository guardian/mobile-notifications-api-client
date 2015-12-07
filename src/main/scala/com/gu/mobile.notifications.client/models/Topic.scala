package com.gu.mobile.notifications.client.models

import com.gu.mobile.notifications.client.models.Editions._
import com.gu.mobile.notifications.client.models.TopicTypes._
import play.api.libs.json._

sealed trait TopicType

object TopicType {
  implicit val jf = new Writes[TopicType] {
    override def writes(o: TopicType): JsValue = JsString(o.toString)
  }
}

object TopicTypes {
  case object Breaking extends TopicType { override def toString = "breaking" }
  case object Content extends TopicType { override def toString = "content" }
  case object TagContributor extends TopicType { override def toString = "tag-contributor" }
  case object TagKeyword extends TopicType { override def toString = "tag-keyword" }
  case object TagSeries extends TopicType { override def toString = "tag-series" }
  case object TagBlog extends TopicType { override def toString = "tag-blog" }
  case object FootballTeam extends TopicType { override def toString = "football-team" }
  case object FootballMatch extends TopicType { override def toString = "football-match" }
  case object User extends TopicType { override def toString = "user-type" }
  case object Newsstand extends TopicType { override def toString = "newsstand" }
}

case class Topic(`type`: TopicType, name: String) {
  def toTopicString = `type`.toString + "//" + name
}
object Topic {
  implicit val jf = Json.writes[Topic]
  val BreakingNewsUk = Topic(Breaking, UK.toString)
  val BreakingNewsUs = Topic(Breaking, US.toString)
  val BreakingNewsAu = Topic(Breaking, AU.toString)
  val BreakingNewsInternational = Topic(Breaking, International.toString)
  val BreakingNewsSport = Topic(Breaking, "sport")
  val NewsstandIos = Topic(Newsstand, "newsstandIos")
}

