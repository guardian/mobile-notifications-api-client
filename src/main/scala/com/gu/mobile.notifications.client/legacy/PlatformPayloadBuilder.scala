package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models.{ExternalLink, GuardianLinkDetails, Link}


trait PlatformPayloadBuilder {

  sealed trait PlatformUriType

  case object Item extends PlatformUriType {
    override def toString = "item"
  }

  case object FootballMatch extends PlatformUriType {
    override def toString = "football-match"
  }

  case object External extends PlatformUriType {
    override def toString = "external"
  }

  case class PlatformUri(uri: String, `type`: PlatformUriType)

  implicit class enrichedURI(uri: URI) {
    def path: Option[String] = toStringOption(uri.getPath)

    def query: Option[String] = toStringOption(uri.getQuery)

    private def toStringOption(s: String) = if (s != null) Some(s) else None
  }

  protected def replaceHost(uri: URI) = List(Some("x-gu://"), uri.path, uri.query.map("?" + _)).flatten.mkString

  protected def toPlatformLink(link:Link) = link match {
      //TODO should we still use the short url? can I use it for android as well as for iOS?
    case GuardianLinkDetails(_, Some(shortUrl), _, _, _, _) => PlatformUri("x-gu:///items" + new URI(shortUrl).getPath, Item)
    case GuardianLinkDetails(contentApiId, _, _, _, _, _) => PlatformUri(s"x-gu:///items/$contentApiId", Item)
    case ExternalLink(url) => PlatformUri(url, External)
  }

  protected def mapWithOptionalValues(elems: (String, String)*)(optionals: (String, Option[String])*) = elems.toMap ++ optionals.collect { case (k, Some(v)) => k -> v }
}
