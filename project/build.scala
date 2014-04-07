import sbt._, Keys._

object Expressier extends Build {
  lazy val demo: Project = Project(
    "expressier-demo",
    file("demo"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)(
        "org.scala-lang" % "scala-reflect" % _
      ),
      libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      resolvers += Resolver.sonatypeRepo("releases"),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M7" cross CrossVersion.full),
      libraryDependencies += "org.scalamacros" %% "quasiquotes" % "2.0.0-M7"
    )
  )

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.0.0-SNAPSHOT",
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    )
  )
}

