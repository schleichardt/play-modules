import play.Project._
import com.typesafe.sbt.SbtScalariform._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import Utilities._
import com.typesafe.sbt.SbtPgp.PgpKeys._

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val root = Project(id = "play-modules", base = file(".")).
  settings(ScctPlugin.mergeReportSettings: _*).
  aggregate(basicAuth, featureToggle, embedMongo, mail)

lazy val basicAuth = project.settings(
  name := "play-2-basic-auth",
  libraryDependencies += javaCore
).settings(commonSettings:_*)

lazy val featureToggle = project.settings(
  name := "play-2-feature-toggle",
  libraryDependencies += javaCore
).settings(commonSettings:_*)

lazy val embedMongo = project.settings(
  name := "play-2-embed-mongo",
  libraryDependencies += "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.43",
  libraryDependencies += "org.mongodb" % "mongo-java-driver" % "2.11.3" % "test",
  parallelExecution in ScctPlugin.ScctTest := false,
  parallelExecution in Test := false
).settings(commonSettings:_*)

lazy val mail = project.settings(
  name := "play-2-mail",
  libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2",
  libraryDependencies += "com.icegreen" % "greenmail" % "1.3" % "test"
).settings(commonSettings:_*)

organization in ThisBuild := "info.schleichardt"

javacOptions in ThisBuild ++= Seq("-source", "1.6", "-target", "1.6") //for compatibility with Debian Squeeze

publishMavenStyle in ThisBuild := true

publishArtifact in Test in ThisBuild := false

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

val commonSettings = scalariformSettings ++ ScctPlugin.instrumentSettings ++ releaseSettings ++ Seq(
  versionFile := (baseDirectory).value / "version.sbt",
  commitMessage := commitMessage.value + " of " + name.value,
  tagName := name .value+ "-" + tagName.value,
  useGlobalVersion := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts.copy(action = publishSignedAction),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  publishTo <<= version {
    (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

//from https://github.com/sbt/sbt-release/issues/49
lazy val publishSignedAction = { st: State =>
  val extracted = st.extract
  val ref = extracted.get(thisProjectRef)
  extracted.runAggregated(publishSigned in Global in ref, st)
}
