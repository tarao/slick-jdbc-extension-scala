import sbt._
import Keys._

lazy val slickjdbcextension = (project in file(".")).
  settings(
    name := "slick-jdbc-extension",
    version := "0.0.1",
    scalaVersion := "2.11.6",

    // Depenency
    resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.typesafe.slick" %% "slick" % "3.0.0-RC3",
      "com.github.tarao" %% "nonempty" % "0.0.1",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
    ),

    // Compilation
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    )
  )
