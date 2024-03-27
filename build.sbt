lazy val V = _root_.scalafix.sbt.BuildInfo

val Scala2Version = "2.13.13"
val Scala3Version = "3.4.0"

inThisBuild(
  List(
    organization := "org.virtuslab",
    homepage := Some(url("https://github.com/VirtusLabRnD/scalafix-migrate-circe-generic-extras")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer("WojciechMazur", "Wojciech Mazur", "wmazur@virtuslab.com", url("https://github.com/WojciechMazur"))
    ),
    scalaVersion := Scala2Version,
    semanticdbEnabled := true,
    semanticdbIncludeInJar := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

(publish / skip) := true

lazy val rules = project.settings(
  moduleName := "scalafix-migrate-circe-generic-extras",
  libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion
)

// Dependencies mostly used to check compilation of circe-generic sources
val circeVersion = "0.14.6"
val circeGenericExtrasVersion = "0.14.3"
val circeDerivationVersion = "0.13.0-M5"
val jawnVersion = "1.5.1"
val munitVersion = "0.7.29"
val disciplineMunitVersion = "1.0.9"
val enumeratumVersion = "1.7.3"

lazy val commonTestDependencies = List(
  // used in examples
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  "com.beachape" %% "enumeratum-circe" % enumeratumVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  // Used by circe-generic-extra tests
  "io.circe" %% "circe-testing" % circeVersion,
  "org.scalameta" %% "munit" % munitVersion,
  "org.scalameta" %% "munit-scalacheck" % munitVersion,
  "org.typelevel" %% "discipline-munit" % disciplineMunitVersion,
  "org.typelevel" %% "jawn-parser" % jawnVersion
)

lazy val input = project.settings(
  (publish / skip) := true,
  scalaVersion := Scala2Version,
  scalacOptions ++= Seq(
    "-Ymacro-annotations"
  ),
  libraryDependencySchemes ++= Seq(
    "io.circe" %% "circe-core" % VersionScheme.Always // See https://github.com/circe/circe-derivation/issues/346
  ),
  libraryDependencies ++= commonTestDependencies ++ Seq(
    "io.circe" %% "circe-derivation" % circeDerivationVersion,
    "io.circe" %% "circe-derivation-annotations" % circeDerivationVersion,
    "io.circe" %% "circe-generic-extras" % circeGenericExtrasVersion
  )
)

lazy val output = project.settings(
  (publish / skip) := true,
  scalaVersion := Scala3Version,
  libraryDependencies ++= commonTestDependencies ++ Seq(
    "io.circe" %% "circe-generic" % circeVersion
  )
)

lazy val tests = project
  .settings(
    (publish / skip) := true,
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      (output / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (input / Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (input / Compile / fullClasspath).value
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)