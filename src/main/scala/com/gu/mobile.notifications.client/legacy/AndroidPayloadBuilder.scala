package com.gu.mobile.notifications.client.legacy

import com.gu.mobile.notifications.client.models.Editions.Edition
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.client.models.legacy.AndroidMessagePayload
import com.gu.mobile.notifications.client.models.legacy.AndroidMessageTypes.{Custom, GoalAlert}
import com.gu.mobile.notifications.client.models.legacy.AndroidKeys.{NotificationType => NotificationTypeKey, Edition => EditionKey, Editions => EditionsKey, Link => LinkKey, _}

import scala.PartialFunction._


object AndroidPayloadBuilder {
  def build(np: NotificationPayload, editions: Set[Edition] = Set.empty): AndroidMessagePayload = np match {
    case ga: GoalAlertPayload => buildGoalAlert(ga)
    case ca: ContentAlertPayload => buildContentAlert(ca)
    case bn: BreakingNewsPayload => buildBreakingNews(bn, editions)
  }

  private def toAndroidLink(link: Link) = link match {
    case GuardianLinkDetails(contentApiId, _, _, _, _, _) => s"x-gu://www.guardian.co.uk/$contentApiId"
    case ExternalLink(url) => url
  }

  private def buildContentAlert(contentAlert: ContentAlertPayload) = AndroidMessagePayload(
    Map(
      Type -> Custom,
      Title -> contentAlert.title,
      Ticker -> contentAlert.message,
      Message -> contentAlert.message,
      LinkKey -> toAndroidLink(contentAlert.link),
      Topics -> contentAlert.topic.map(_.toTopicString).mkString(",")
    ) ++ Seq(
      ImageUrl -> contentAlert.imageUrl.map(_.toString),
      ThumbnailUrl -> contentAlert.thumbnailUrl.map(_.toString)
    ).collect({
      case (k, Some(v)) => k -> v
    })
  )

  private def buildGoalAlert(goalAlert: GoalAlertPayload) = AndroidMessagePayload(Map(
    Type -> GoalAlert,
    AwayTeamName -> goalAlert.awayTeamName,
    AwayTeamScore -> goalAlert.awayTeamScore.toString,
    HomeTeamName -> goalAlert.homeTeamName,
    HomeTeamScore -> goalAlert.homeTeamScore.toString,
    ScoringTeamName -> goalAlert.scoringTeamName,
    ScorerName -> goalAlert.scorerName,
    GoalMins -> goalAlert.goalMins.toString,
    OtherTeamName -> goalAlert.otherTeamName,
    MatchId -> goalAlert.matchId,
    MapiUrl -> goalAlert.mapiUrl.toString,
    Debug -> goalAlert.debug.toString
  ))

  private def buildBreakingNews(breakingNews: BreakingNewsPayload, editions: Set[Edition]) = {

    val sectionLink = condOpt(breakingNews.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITSection, _) => contentApiId
    }

    val tagLink = condOpt(breakingNews.link) {
      case GuardianLinkDetails(contentApiId, _, _, _, GITTag, _) => contentApiId
    }

    AndroidMessagePayload(
      Map(
        Type -> Custom,
        NotificationTypeKey -> breakingNews.`type`.toString,
        Title -> breakingNews.title,
        Ticker -> breakingNews.message,
        Message -> breakingNews.message,
        Debug -> breakingNews.debug.toString,
        EditionsKey -> editions.mkString(","),
        LinkKey -> toAndroidLink(breakingNews.link),
        Topics -> breakingNews.topic.map(_.toTopicString).mkString(",")
      ) ++ Seq(
        Section -> sectionLink,
        EditionKey -> (if (editions.size == 1) Some(editions.head.toString) else None),
        Keyword -> tagLink,
        ImageUrl -> breakingNews.imageUrl.map(_.toString),
        ThumbnailUrl -> breakingNews.thumbnailUrl.map(_.toString)
      ).collect({
        case (k, Some(v)) => k -> v
      })
    )
  }
}
