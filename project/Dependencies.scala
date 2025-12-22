import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  val Versions = new {
    val laminar = "17.2.1"
    val logbackClassic = "1.5.23"
    val webawesome = "3.0.0"
    val tapir = "1.13.3"
    val sttp = "4.0.13"
    val sttpModelCore = "1.7.17"
    val zio = "2.1.23"
    val zioJson = "0.7.45"
    val zioPrelude = "1.0.0-RC42"
  }

  val coreDependencies =
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % Versions.laminar,
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.client4" %%% "zio" % Versions.sttp
    )
  val exampleClientDependencies =
    libraryDependencies ++= Seq(
      "io.github.nguyenyou" %%% "webawesome-laminar" % Versions.webawesome
    )
}
