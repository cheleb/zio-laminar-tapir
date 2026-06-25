import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  val Versions = new {
    val laminar = "18.0.0-M3"
    val logbackClassic = "1.5.32"
    val webawesome = "3.2.1"
    val tapir = "1.13.24"
    val sttp = "4.0.25"
    val sttpModelCore = "1.7.17"
    val zio = "2.1.26"
    val zioJson = "0.9.2"
    val zioPrelude = "1.0.0-RC44"
    val zioSchema = "1.8.5"
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
