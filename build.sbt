
name := "mobile-notifications-client"

organization := "com.gu"

scalaVersion := "2.10.3"

version := "0.3-SNAPSHOT"

resolvers ++= Seq(
  "Guardian GitHub Releases" at "http://guardian.github.io/maven/repo-releases",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.2.0",
  "net.databinder.dispatch" %% "dispatch-core" % "0.10.0",
  "org.specs2" %% "specs2" % "2.3.4" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.33" % "test"
)

publishTo <<= (version) { version: String =>
  val publishType = if (version.endsWith("SNAPSHOT")) "snapshots" else "releases"
  Some(
    Resolver.file(
      "guardian github " + publishType,
      file(System.getProperty("user.home") + "/guardian.github.com/maven/repo-" + publishType)
    )
  )
}
