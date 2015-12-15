package com.gu.mobile.notifications.client.lib

import java.net.URL

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

import scala.util.Try

object JsonFormatsHelper {
  implicit class RichJsObject(obj: JsObject) {
    def +:(kv: (String, JsValue)): JsObject = obj match {
      case JsObject(entries) => JsObject(entries + kv)
    }
  }

  implicit class RichWrites[A](writes: Writes[A]) {
    def withTypeString(typ: String): Writes[A] = writes transform { _ match {
      case obj: JsObject => ("type" -> JsString(typ)) +: obj
      case x: JsValue => x
    }}
  }

  implicit class RichReads[A](innerReads: Reads[A]) {
    def withTypeString(typ: String): Reads[A] = new Reads[A] {
      def reads(json: JsValue): JsResult[A] = json match {
        case obj: JsObject if obj.value.get("type") == Some(JsString(typ)) => innerReads.reads(obj - "type")
        case _: JsObject => JsError(s"Not of type $typ")
        case _ => JsError("Not an object")
      }
    }
  }

  implicit class RichFormat[A](format: Format[A]) {
    def withTypeString(typ: String): Format[A] = new Format[A] {
      private val richWrites = new RichWrites[A](format).withTypeString(typ)
      private val richReads = new RichReads[A](format).withTypeString(typ)
      override def writes(o: A): JsValue = richWrites.writes(o)
      override def reads(json: JsValue): JsResult[A] = richReads.reads(json)
    }
  }

  class TypedSubclassReads[A](val subClassReads: List[Reads[_ <: A]]) extends Reads[A] {
    def reads(json: JsValue): JsResult[A] = {
      def iter(reads: List[Reads[_ <: A]]): JsResult[A] = reads match {
        case x :: xs => x.reads(json) orElse iter(xs)
        case Nil => JsError("No match for typed subclasses")
      }

      iter(subClassReads)
    }
  }

  implicit val urlFormat = new Format[URL] {
    override def writes(o: URL): JsValue = JsString(o.toExternalForm)
    override def reads(json: JsValue): JsResult[URL] = json match {
      case JsString(url) => Try(JsSuccess(new URL(url))).getOrElse(JsError(s"Invalid url: $url"))
      case _ => JsError("Invalid url type")
    }
  }
}