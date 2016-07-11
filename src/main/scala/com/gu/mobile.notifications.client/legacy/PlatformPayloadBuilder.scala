package com.gu.mobile.notifications.client.legacy

import java.net.URI

import com.gu.mobile.notifications.client.models.{ExternalLink, GuardianLinkDetails, Link}


trait PlatformPayloadBuilder {

  implicit class enrichedURI(uri: URI) {
    def path: Option[String] = toStringOption(uri.getPath)

    def query: Option[String] = toStringOption(uri.getQuery)

    private def toStringOption(s: String) = if (s != null) Some(s) else None
  }

  protected def replaceHost(uri: URI) = List(Some("x-gu://MAPI"), uri.path, uri.query.map("?" + _)).flatten.mkString

  protected def toMapiLink(link: Link) = link match {
    case GuardianLinkDetails(contentApiId, _, _, _, _, _) => Some(s"x-gu://MAPI/items/$contentApiId")
    case ExternalLink(url) => None
  }

  protected def mapWithOptionalValues(elems: (String, String)*)(optionals: (String, Option[String])*) = elems.toMap ++ optionals.collect { case (k, Some(v)) => k -> v }


}
