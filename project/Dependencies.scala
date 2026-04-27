import sbt._
import sbt.Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  val Versions = new {
    val laminar = "17.2.1"
    val logbackClassic = "1.5.32"
    val webawesome = "3.2.1"
    val opentelemetry = "1.61.0"
    val opentelemetryRuntime = "2.27.0-alpha"
    val opentelemetrySemcov = "1.40.0"
    val tapir = "1.13.17"
    val sttp = "4.0.23"
    val sttpModelCore = "1.7.17"
    val zio = "2.1.25"
    val zioJson = "0.7.45"
    val zioLogging = "2.5.2"
    val zioOpenTelemetry = "3.1.15"
    val zioPrelude = "1.0.0-RC44"
  }

  val serverDependencies =
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % Versions.logbackClassic,
      "dev.zio" %% "zio-json" % Versions.zioJson,
      "dev.zio" %% "zio-logging-slf4j2" % Versions.zioLogging,
      "dev.zio" %% "zio-opentelemetry" % Versions.zioOpenTelemetry,
      "dev.zio" %% "zio-opentelemetry-zio-logging" % Versions.zioOpenTelemetry,
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-opentelemetry-tracing" % Versions.tapir,
      "io.opentelemetry" % "opentelemetry-context" % Versions.opentelemetry,
      "io.opentelemetry" % "opentelemetry-sdk" % Versions.opentelemetry,
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % Versions.opentelemetry,
      "io.opentelemetry" % "opentelemetry-exporter-logging-otlp" % Versions.opentelemetry,
      "io.opentelemetry.semconv" % "opentelemetry-semconv" % Versions.opentelemetrySemcov,
      "io.opentelemetry.instrumentation" % "opentelemetry-runtime-telemetry-java17" % Versions.opentelemetryRuntime
    )

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
