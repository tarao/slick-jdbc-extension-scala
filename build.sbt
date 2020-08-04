import sbt._
import Keys._

lazy val slickjdbcextension = (project in file(".")).
  settings(
    name := "slick-jdbc-extension",
    organization := "com.github.tarao",
    version := "0.1.1-SNAPSHOT",
    scalaVersion := "2.13.0",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0"),

    // Depenency
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.typesafe.slick" %% "slick" % "3.3.2",
      "eu.timepit" %% "refined"  % "0.9.15",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
      "org.scalamock" %% "scalamock" % "5.0.0" % "test",
      "com.h2database" % "h2" % "1.4.200" % "test"
    ),

    // Compilation
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    // Documentation
    scalacOptions in (Compile, doc) ++= Nil :::
      "-groups" ::
      "-sourcepath" ::
      baseDirectory.value.getAbsolutePath ::
      "-doc-source-url" ::
      "https://github.com/tarao/slick-jdbc-extension-scala/tree/master€{FILE_PATH}.scala" ::
      Nil,

    // Publishing
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/tarao/slick-jdbc-extension-scala</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:tarao/slick-jdbc-extension-scala.git</url>
        <connection>scm:git:git@github.com:tarao/slick-jdbc-extension-scala.git</connection>
      </scm>
      <developers>
        <developer>
          <id>tarao</id>
          <name>INA Lintaro</name>
          <url>https://github.com/tarao/</url>
        </developer>
      </developers>)
  )
