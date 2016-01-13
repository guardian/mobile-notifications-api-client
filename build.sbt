import sbtrelease._
import ReleaseStateTransformations._

name := "mobile-notifications-client"

organization := "com.gu"

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.3", "2.10.4", "2.11.2")

resolvers ++= Seq(
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.2",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1" % Test,
  "org.specs2" %% "specs2" % "2.3.13" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.33" % "test"
)

//releaseSettings

//sonatypeSettings

description := "Scala client for the Guardian Push Notifications API"

scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/mobile-notifications-api-client"),
  "scm:git:git@github.com:guardian/mobile-notifications-api-client.git"
))

pomExtra in Global := {
  <url>https://github.com/guardian/mobile-notifications-api-client</url>
    <developers>
      <developer>
        <id>robertberry</id>
        <name>Robert Berry</name>
        <url>https://github.com/robertberry</url>
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
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
