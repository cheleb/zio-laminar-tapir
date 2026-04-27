package dev.cheleb.ziotapir.server.otel

import io.opentelemetry.context.Context
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.{RequestInterceptor, RequestResult}
import zio.Task
import zio.telemetry.opentelemetry.context.ContextStorage

/** Propagates OpenTelemetry's [[io.opentelemetry.context.Context]]
  * (ThreadLocal) into zio-opentelemetry's [[ContextStorage]] (FiberRef) for
  * each Tapir request, so that [[zio.telemetry.opentelemetry.tracing.Tracing]]
  * spans (e.g. `tracing.span`) are children of the HTTP
  * [[sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing]] `SERVER`
  * span.
  *
  * Pass the live [[ContextStorage]] from the ZIO program (e.g.
  * `ZIO.service[ContextStorage]`) and prepend the returned interceptor
  * ''after'' `OpenTelemetryTracing(otel)` so the list is
  * `[ OpenTelemetryTracing, this bridge, … ]`. The bridge then runs inside
  * Tapir's `withSpan` so [[Context.current]] is still the request span when the
  * effect transform runs.
  *
  * **Manual verification in Jaeger / Tempo / Grafana (LGTM)**: start the
  * simulator with the OTLP exporter pointing at a collector, call an
  * instrumented route (e.g. Kiln `defi-network-stats` with `tracing.span`), and
  * confirm a single trace where application spans are children of the `SERVER`
  * span.
  */
object ZioOtelContextBridge {

  def tapirRequestInterceptor(
      storage: ContextStorage
  ): RequestInterceptor[Task] =
    RequestInterceptor.transformResultEffect[Task](
      new RequestInterceptor.RequestResultEffectTransform[Task] {
        def apply[B](
            request: ServerRequest,
            result: Task[RequestResult[B]]
        ): Task[RequestResult[B]] = {
          // Captured while `OpenTelemetryTracing` still has the server `Span` in `Context` (synchronous transform hook).
          val ctx: Context = Context.current()
          storage.locally(ctx)(result)
        }
      }
    )
}
