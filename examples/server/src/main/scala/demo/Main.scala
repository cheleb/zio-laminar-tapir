package demo

import zio.*
import zio.http.*

import sttp.tapir.server.ziohttp.ZioHttpInterpreter

import zio.telemetry.opentelemetry.tracing.Tracing
import zio.telemetry.opentelemetry.OpenTelemetry
import sttp.tapir.server.tracing.ziotel.ZIOpenTelemetry

object Main extends ZIOApp with ZIOpenTelemetry("zio-tapir-server") {

  // The main program - start the server on port 8080
  val program: ZIO[Tracing & Server, Throwable, Unit] = for
    _ <- Console.printLine("Starting server on http://localhost:8080")

    given Tracing <- ZIO.service[Tracing]

    endpointsNoDeps <- HttpApiAny.endpoints

    endpoints <- HttpApi().endpointsWithDeps

    httpApp = ZioHttpInterpreter(serverOptions).toHttp(
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
