import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  val Versions = new {
    val laminar = "17.2.1"
    val logbackClassic = "1.6.6"
    val webawesome = "3.14.1"
    val tapir = "1.14.1"
    val sttp = "4.1.1"
    val sttpModelCore = "1.9.0"
    val zio = "2.1.27"
    val zioPrelude = "1.0.0-RC48"
  }

  val coreDependencies =
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.client4" %%% "zio" % Versions.sttp
    )

  val laminarDependencies =
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % Versions.laminar
    )
  val exampleClientDependencies =
    libraryDependencies ++= Seq(
      "io.github.nguyenyou" %%% "webawesome-laminar" % Versions.webawesome
    )
}
