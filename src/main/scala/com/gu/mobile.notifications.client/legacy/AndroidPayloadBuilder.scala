package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.AndroidMessagePayload
import com.gu.mobile.notifications.client.models.legacy.AndroidMessageTypes.{Custom, GoalAlert}
import com.gu.mobile.notifications.client.models.legacy.{AndroidKeys => keys}

import scala.PartialFunction._

object AndroidPayloadBuilder extends PlatformPayloadBuilder{
  def build(np: NotificationPayload, editions: Set[Edition] = Set.empty): AndroidMessagePayload = np match {
    case ga: GoalAlertPayload => buildGoalAlert(ga)
    case ca: ContentAlertPayload => buildContentAlert(ca)
    case bn: BreakingNewsPayload => buildBreakingNews(bn, editions)
  }

  private def toAndroidLink(link: Link) = link match {
    case GuardianLinkDetails(contentApiId, _, _, _, _, _) => s"x-gu://www.guardian.co.uk/$contentApiId"
    case ExternalLink(url) => url
  }

  private def toMapiGoalAlertLink(uri: URI) = if (uri.getHost.startsWith("football")) None else Some(replaceHost(uri))

  private def buildContentAlert(contentAlert: ContentAlertPayload) = AndroidMessagePayload(
    mapWithOptionalValues(
      keys.Type -> Custom,
      keys.uniqueIdentifier -> contentAlert.derivedId,
      keys.Title -> contentAlert.title,
      keys.Ticker -> contentAlert.message,
      keys.Message -> contentAlert.message,
      keys.Link -> toAndroidLink(contentAlert.link),
      keys.Topics -> contentAlert.topic.map(_.toTopicString).mkString(",")
    )(
      keys.ImageUrl -> contentAlert.imageUrl.map(_.toString),
      keys.ThumbnailUrl -> contentAlert.thumbnailUrl.map(_.toString),
      keys.MapiLink -> toMapiLink(contentAlert.link)
    )
  )

  private def buildGoalAlert(goalAlert: GoalAlertPayload) = AndroidMessagePayload(
    mapWithOptionalValues(
      keys.Type -> GoalAlert,
      keys.uniqueIdentifier -> goalAlert.derivedId,
      keys.AwayTeamName -> goalAlert.awayTeamName,
      keys.AwayTeamScore -> goalAlert.awayTeamScore.toString,
      keys.HomeTeamName -> goalAlert.homeTeamName,
      keys.HomeTeamScore -> goalAlert.homeTeamScore.toString,
      keys.ScoringTeamName -> goalAlert.scoringTeamName,
      keys.ScorerName -> goalAlert.scorerName,
      keys.GoalMins -> goalAlert.goalMins.toString,
      keys.OtherTeamName -> goalAlert.otherTeamName,
      keys.MatchId -> goalAlert.matchId,
      keys.MapiUrl -> goalAlert.mapiUrl.toString,
      keys.Debug -> goalAlert.debug.toString
    )(keys.MapiLink -> toMapiGoalAlertLink(goalAlert.mapiUrl))
  )

  private def buildBreakingNews(breakingNews: BreakingNewsPayload, editions: Set[Edition]) = {

    val sectionLink = condOpt(breakingNews.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITSection, _) => contentApiId
    }

    val tagLink = condOpt(breakingNews.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITTag, _) => contentApiId
    }

    AndroidMessagePayload(
      mapWithOptionalValues(
        keys.Type -> Custom,
        keys.uniqueIdentifier -> breakingNews.id,
        keys.NotificationType -> breakingNews.`type`.toString,
        keys.Title -> breakingNews.title,
        keys.Ticker -> breakingNews.message,
        keys.Message -> breakingNews.message,
        keys.Debug -> breakingNews.debug.toString,
        keys.Editions -> editions.mkString(","),
        keys.Link -> toAndroidLink(breakingNews.link),
        keys.Topics -> breakingNews.topic.map(_.toTopicString).mkString(",")
      )(
        keys.Section -> sectionLink,
        keys.Edition -> (if (editions.size == 1) Some(editions.head.toString) else None),
        keys.Keyword -> tagLink,
        keys.ImageUrl -> breakingNews.imageUrl.map(_.toString),
        keys.ThumbnailUrl -> breakingNews.thumbnailUrl.map(_.toString),
        keys.MapiLink -> toMapiLink(breakingNews.link)
      )
    )
  }
}
