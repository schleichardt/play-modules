import play.Project._

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val basicAuth = project.settings(
  version := "0.3-SNAPSHOT",
  name := "play-2-basic-auth",
  libraryDependencies += javaCore
)

lazy val featureToggle = project.settings(
  version := "0.1-SNAPSHOT",
  name := "play-2-feature-toggle",
  libraryDependencies += javaCore
)

lazy val mail = project.settings(
  version := "1.0-SNAPSHOT",
  name := "play-2-mail",
  libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2",
  libraryDependencies += "com.icegreen" % "greenmail" % "1.3" % "test"
)

organization in ThisBuild := "info.schleichardt"

javacOptions in ThisBuild ++= Seq("-source", "1.6", "-target", "1.6") //for compatibility with Debian Squeeze

publishMavenStyle in ThisBuild := true

publishArtifact in Test in ThisBuild := false

publishTo in ThisBuild <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

libraryDependencies in ThisBuild += "com.typesafe.play" %% "play" % play.core.PlayVersion.current

libraryDependencies in ThisBuild += "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test"

pomIncludeRepository in ThisBuild := {
  _ => false
}

pomExtra in ThisBuild := (
  <url>https://github.com/schleichardt/play-modules
  </url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:schleichardt/play-modules.git</url>
      <connection>scm:git:git@github.com:schleichardt/play-modules.git</connection>
    </scm>
    <developers>
      <developer>
        <id>schleichardt</id>
        <name>Michael Schleichardt</name>
        <url>http://michael.schleichardt.info</url>
      </developer>
    </developers>
  )
