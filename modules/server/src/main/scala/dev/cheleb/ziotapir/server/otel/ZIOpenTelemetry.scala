package dev.cheleb.ziotapir.server.otel

import zio.*
import zio.telemetry.opentelemetry.context.ContextStorage
import zio.telemetry.opentelemetry.OpenTelemetry
import io.opentelemetry.api
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import zio.logging.backend.SLF4J

import sttp.model.StatusCode
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.ErrorAttributes
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.server.interceptor.exception.DefaultExceptionHandler
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing

/** ZIOpenTelemetry is a trait that provides a ZIO layer for OpenTelemetry.
  * @param name
  */
trait ZIOpenTelemetry(resourceName: String) {
  this: ZIOApp =>

  /** The environment for the ZIOpenTelemetry trait.
    *
    * This is the environment that will be used to run the ZIO application,
    * hence provided by bootstrap. It includes the ContextStorage and the
    * OpenTelemetry instance.
    */
  override type Environment = ContextStorage &
    io.opentelemetry.api.OpenTelemetry

  /** The tag for the ZIOpenTelemetry trait. */
  def environmentTag: Tag[Environment] =
    Tag[Environment]

  /** The OpenTelemetry layer for the ZIOpenTelemetry trait.
    *
    * If the OTEL_EXPORTER_OTLP_ENDPOINT environment variable is set, the
    * OpenTelemetry layer will be created using the OtelSdk.custom method.
    * Otherwise, the OpenTelemetry layer will be created using the
    * OpenTelemetry.contextZIO and ZLayer.succeed(api.OpenTelemetry.noop())
    * methods.
    */
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Environment] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j // Console logger.
      >>> OpenTelemetry.contextZIO >+> otelLayer // OpenTelemetry layer.

  private def otelLayer = sys.env
    .get(
      "OTEL_EXPORTER_OTLP_ENDPOINT"
    ) match
    case Some(endpoint) =>
      OtelSdk
        .custom(resourceName) >+> OpenTelemetry
        .logging(s"zio-simulator-${resourceName}")
    case None =>
      ZLayer
        .succeed(api.OpenTelemetry.noop())

  def exceptionHandler(exception: Throwable): ExceptionHandler[Task] =
    DefaultExceptionHandler[Task]

  /** The server options for the ZIOpenTelemetry trait.
    *
    * This is the server options that will be used to run the ZIO application,
    * hence provided by bootstrap. It includes the OpenTelemetry instance and
    * the ContextStorage.
    */
  protected def serverOptions(
      otel: api.OpenTelemetry,
      contextStorage: ContextStorage
  ): ZioHttpServerOptions[Any] =
    ZioHttpServerOptions.customiseInterceptors
      .prependInterceptor(
        OpenTelemetryTracing(otel)
      )
      .prependInterceptor(
        ZioOtelContextBridge.tapirRequestInterceptor(contextStorage)
      )
      .appendInterceptor(
        CORSInterceptor.default
      )
      .serverLog(
        ZioHttpServerOptions.defaultServerLog
      )
      .options

  def customErrorAttributes(e: Either[StatusCode, Throwable]): Attributes =
    e match {
      case Left(statusCode) =>
        // see footnote for error.type
        Attributes
          .builder()
          .put(ErrorAttributes.ERROR_TYPE, statusCode.code.toString)
          .build()
      case Right(exception) => {
        val errorType = exception.getClass.getSimpleName
        Attributes
          .builder()
          .put(ErrorAttributes.ERROR_TYPE, errorType)
          .put("exception.message", exception.getMessage)
          .put("exception.stacktrace", exception.getStackTrace.mkString("\n"))
          .build()
      }
    }
}
