import sbt._, Keys._

object Expressier extends Build {
  lazy val demo: Project = Project(
    "expressier-demo",
    file("demo"),
    settings = buildSettings ++ Seq(
      libraryDependencies <+= (scalaVersion)(
        "org.scala-lang" % "scala-reflect" % _
      )
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

