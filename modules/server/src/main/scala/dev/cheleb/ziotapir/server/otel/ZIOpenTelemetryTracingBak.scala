package dev.cheleb.ziotapir.server.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.{Span, SpanKind, Tracer}

import io.opentelemetry.context.propagation.{ContextPropagators, TextMapGetter}
import sttp.monad.MonadError
import sttp.monad.syntax._
import sttp.tapir.AnyEndpoint
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.RequestResult.{Failure, Response}
import sttp.tapir.server.interceptor.{
  DecodeFailureContext,
  DecodeSuccessContext,
  EndpointHandler,
  EndpointInterceptor,
  RequestHandler,
  RequestInterceptor,
  RequestResult,
  Responder,
  SecurityFailureContext
}
import sttp.tapir.server.interpreter.BodyListener
import sttp.tapir.server.model.ServerResponse

import scala.jdk.CollectionConverters._
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracingConfig

import zio.*
import zio.telemetry.opentelemetry.context.ContextStorage
import io.opentelemetry.context.Context
//import io.opentelemetry.context.Context

/** Interceptor which traces requests using OpenTelemetry.
  *
  * Span names and attributes are calculated using the provided
  * [[OpenTelemetryTracingConfig]].
  *
  * To use, customize the interceptors of the server interpreter you are using,
  * and prepend this interceptor, so that it runs as early as possible, e.g.:
  *
  * {{{
  * val otel: OpenTelemetry = ???
  * val serverOptions: NettySyncServerOptions =
  *    NettySyncServerOptions.customiseInterceptors
  *      .prependInterceptor(OpenTelemetryTracing(otel))
  *      .options
  * }}}
  *
  * Relies on the built-in OpenTelemetry Java SDK
  * [[io.opentelemetry.context.ContextStorage]] mechanism of propagating the
  * tracing context; by default, this is using [[ThreadLocal]]s, which works
  * with synchronous/direct-style environments. [[Future]]s are supported
  * through instrumentation provided by the OpenTelemetry javaagent. For
  * functional effect systems, usually a dedicated integration library is
  * required.
  */
class ZIOpenTelemetryTracing(
    config: OpenTelemetryTracingConfig,
    contextStorage: ContextStorage
) extends RequestInterceptor[Task] {

  private val getter = new TextMapGetter[ServerRequest] {
    override def get(carrier: ServerRequest, key: String): String =
      carrier.header(key).getOrElse(null)
    override def keys(carrier: ServerRequest): java.lang.Iterable[String] =
      carrier.headers.map(_.name).asJava
  }

  override def apply[R, B](
      responder: Responder[Task, B],
      requestHandler: EndpointInterceptor[Task] => RequestHandler[Task, R, B]
  ): RequestHandler[Task, R, B] = new RequestHandler[Task, R, B] {
    override def apply(
        request: ServerRequest,
        endpoints: List[ServerEndpoint[R, Task]]
    )(implicit
        monad: MonadError[Task]
    ): Task[RequestResult[B]] = withPropagatedContext(request) {
      val span = config.tracer
        .spanBuilder(config.spanName(request))
        .setAllAttributes(config.requestAttributes(request))
        .setSpanKind(SpanKind.SERVER)
        .startSpan()

      withSpan(span) {
        requestHandler(knownEndpointInterceptor(request, span))(
          request,
          endpoints
        )
          .map { result =>
            result match {
              case Response(response, _) =>
                span.setAllAttributes(
                  config.responseAttributes(request, response)
                )
                // https://opentelemetry.io/docs/specs/semconv/http/http-spans/#status
                if (response.isServerError) {
                  span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR)
                  val _ = span.setAllAttributes(
                    config.errorAttributes(Left(response.code))
                  )
                }
              case Failure(_) => // ignore, request not handled
            }

            result
          }
          .handleError { case e: Exception =>
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR)
            span.setAllAttributes(config.errorAttributes(Right(e)))
            monad.error(e)
          }
      }
    }
  }

  private def withPropagatedContext[T](
      request: ServerRequest
  )(f: => Task[T]): Task[T] = {
    for
      ctx = Context.root()
      amendedContext = config.propagators
        .getTextMapPropagator()
        .extract(ctx, request, getter)

      res <- contextStorage.locally(amendedContext)(
        f
      ) // ensure the extracted context is visible in the ZIO environment for downstream effects
    yield res
  }

  private def withSpan[T](
      span: Span
  )(f: => Task[T]): Task[T] = {
    contextStorage.updateAndGet(ctx => span.storeInContext(ctx)).flatMap { _ =>
      f
    }
  }

  private def knownEndpointInterceptor(request: ServerRequest, span: Span) =
    new EndpointInterceptor[Task] {
      def apply[B](
          responder: Responder[Task, B],
          endpointHandler: EndpointHandler[Task, B]
      ): EndpointHandler[Task, B] = {
        new EndpointHandler[Task, B] {
          def onDecodeFailure(
              ctx: DecodeFailureContext
          )(implicit
              monad: MonadError[Task],
              bodyListener: BodyListener[Task, B]
          ): Task[Option[ServerResponse[B]]] = {
            endpointHandler.onDecodeFailure(ctx).map { result =>
              if (result.isDefined) {
                // only setting the attributes if a response has been created using this endpoint
                knownEndpoint(ctx.endpoint)
              }
              result
            }
          }

          def onDecodeSuccess[A, U, I](
              ctx: DecodeSuccessContext[Task, A, U, I]
          )(implicit
              monad: MonadError[Task],
              bodyListener: BodyListener[Task, B]
          ): Task[ServerResponse[B]] = {
            monad
              .eval(knownEndpoint(ctx.endpoint))
              .flatMap(_ => endpointHandler.onDecodeSuccess(ctx))
          }

          def onSecurityFailure[A](
              ctx: SecurityFailureContext[Task, A]
          )(implicit
              monad: MonadError[Task],
              bodyListener: BodyListener[Task, B]
          ): Task[ServerResponse[B]] = {
            monad
              .eval(knownEndpoint(ctx.endpoint))
              .flatMap(_ => endpointHandler.onSecurityFailure(ctx))
          }

          def knownEndpoint(e: AnyEndpoint): Unit = {
            val (name, attributes) =
              config.spanNameFromEndpointAndAttributes(request, e)
            span.updateName(name)
            val _ = span.setAllAttributes(attributes)
          }
        }
      }
    }
}

object ZIOpenTelemetryTracing {

  /** Create the tracing interceptor using the provided configuration. */
  def apply[Task](
      config: OpenTelemetryTracingConfig,
      contextStorage: ContextStorage
  ): ZIOpenTelemetryTracing =
    new ZIOpenTelemetryTracing(config, contextStorage)

  /** Create the tracing interceptor using the default configuration, created
    * using the given [[OpenTelemetry]] instance.
    */
  def apply[Task](
      openTelemetry: OpenTelemetry,
      contextStorage: ContextStorage
  ): ZIOpenTelemetryTracing =
    apply(OpenTelemetryTracingConfig(openTelemetry), contextStorage)

  /** Create the tracing interceptor using the default configuration, created
    * using the given [[Tracer]] instance.
    */
  def apply[Task](
      tracer: Tracer,
      propagators: ContextPropagators,
      contextStorage: ContextStorage
  ): ZIOpenTelemetryTracing = apply(
    OpenTelemetryTracingConfig.usingTracer(tracer, propagators),
    contextStorage
  )
}
