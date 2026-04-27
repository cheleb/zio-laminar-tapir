package demo

import zio.*
import zio.http.*

import io.opentelemetry.api

import sttp.tapir.server.ziohttp.ZioHttpInterpreter

import dev.cheleb.ziotapir.server.otel.ZIOpenTelemetry
import zio.telemetry.opentelemetry.context.ContextStorage
import zio.telemetry.opentelemetry.tracing.Tracing
import zio.telemetry.opentelemetry.OpenTelemetry

object Main extends ZIOApp with ZIOpenTelemetry("zio-tapir-server") {

  // The main program - start the server on port 8080
  val program = for
    _ <- Console.printLine("Starting server on http://localhost:8080")

    otel <- ZIO.service[api.OpenTelemetry]
    contextStorage <- ZIO.service[ContextStorage]
    given Tracing <- ZIO.service[Tracing]

    endpointsNoDeps <- HttpApiAny.endpoints

    endpoints <- HttpApi().endpointsWithDeps

    httpApp = ZioHttpInterpreter(serverOptions(otel, contextStorage)).toHttp(
      endpoints ++ endpointsNoDeps
    )
    _ <- Server.serve(httpApp)
  yield ()

  // Run the program
  override def run =
    program.provideSome[Environment](
      Server.default,
      OpenTelemetry.tracing("DemoServer"),
      OpenTelemetry.metrics("DemoServer"),
      OpenTelemetry.zioMetrics
    )

}
