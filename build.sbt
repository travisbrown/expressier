import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "io.expressier",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  ),

  /** We need the Macro Paradise plugin both to support the macro
    * annotations used in the public type provider implementation and to
    * allow us to use quasiquotes in both implementations. The anonymous
    * type providers could easily (although much less concisely) be
    * implemented without the plugin.
    */
  addCompilerPlugin(paradiseDependency)
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val root =  project.in(file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    initialCommands in console :=
      """
        |import io.expressier._
      """.stripMargin
  )
  .aggregate(core, coreJS)
  .dependsOn(core)

lazy val coreBase = crossProject.in(file("core"))
  .settings(
    description := "expressier core",
    moduleName := "expressier-core",
    name := "core"
  )
  .settings(allSettings: _*)
  .settings(macroProjectSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.2.5",
      "org.typelevel" %%% "macro-compat" % "1.0.1",
      "org.scalatest" %%% "scalatest" % "3.0.0-M7" % "test"
    )
  )
  .settings(macroProjectSettings: _*)
  .jvmConfigure(_.copy(id = "core"))
  .jsConfigure(_.copy(id = "coreJS"))

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val paradiseDependency =
  "org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full

lazy val macroProjectSettings = Seq(
  libraryDependencies <+= (scalaVersion)(
    "org.scala-lang" % "scala-reflect" % _
  ),
  libraryDependencies ++= (
    if (scalaVersion.value.startsWith("2.10")) List(paradiseDependency) else Nil
  )
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/expressier")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/expressier/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/expressier"),
      "scm:git:git@github.com:travisbrown/expressier.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
