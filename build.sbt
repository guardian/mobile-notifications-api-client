import sbtrelease._
import ReleaseStateTransformations._

name := "mobile-notifications-client"

organization := "com.gu"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.11.12", "2.12.4")

releaseCrossBuild := true

resolvers ++= Seq(
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.6.8",
  "org.specs2" %% "specs2-core" % "4.0.2" % "test",
  "org.specs2" %% "specs2-mock" % "4.0.2" % "test"
)

description := "Scala client for the Guardian Push Notifications API"

scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/mobile-notifications-api-client"),
  "scm:git:git@github.com:guardian/mobile-notifications-api-client.git"
))

pomExtra in Global := {
  <url>https://github.com/guardian/mobile-notifications-api-client</url>
    <developers>
      <developer>
        <id>@guardian</id>
        <name>The guardian</name>
        <url>https://github.com/guardian</url>
      </developer>
    </developers>
}

licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
  pushChanges
)
