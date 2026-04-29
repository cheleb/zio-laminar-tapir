package dev.cheleb.ziotapir.server.otel

import zio.*
//import io.opentelemetry.api.trace.{Span, SpanKind, Tracer, StatusCode}

import sttp.monad.MonadError
//import sttp.monad.syntax.MonadErrorOps
import sttp.tapir.AnyEndpoint
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.interceptor.RequestResult.{Failure, Response}
import sttp.tapir.server.interceptor._
import sttp.tapir.server.interpreter.BodyListener
import sttp.tapir.server.model.ServerResponse
import zio.telemetry.opentelemetry.tracing.propagation.TraceContextPropagator
import zio.telemetry.opentelemetry.context.IncomingContextCarrier

import io.opentelemetry.api.trace.Span

import zio.telemetry.opentelemetry.tracing.Tracing
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode

/** Interceptor which traces requests using otel4s.
  *
  * Span names and attributes are calculated using the provided
  * [[Otel4sTracingConfig]].
  *
  * To use, customize the interceptors of the server interpreter you are using,
  * and prepend this interceptor, so that it runs as early as possible, e.g.:
  *
  * {{{
  * OtelJava
  *   .autoConfigured[IO]()
  *   .use { otel4s =>
  *     otel4s.tracerProvider.get("tracer name").flatMap { tracer =>
  *       def endpoints: List[ServerEndpoint[Any, IO]] = ???
  *       val routes =
  *         Http4sServerInterpreter[IO](Http4sServerOptions.default[IO].prependInterceptor(Otel4sTracing(tracing)))
  *           .toRoutes(endpoints)
  *        //...
  *   }
  * }
  * }}}
  * See https://typelevel.org/otel4s/oteljava/tracing-context-propagation.html
  * for details on context propagation.
  */

class ZIOpenTelemetryTracing(
    config: Otel4zTracingConfig,
    propagator: TraceContextPropagator,
    carrier: IncomingContextCarrier[
      scala.collection.mutable.Map[String, String]
    ]
) extends RequestInterceptor[Task] {

  import config.*
  // https://typelevel.org/otel4s/instrumentation/tracing-cross-service-propagation.html
  // implicit private val getter: TextMapGetter[ServerRequest] =
  //   new TextMapGetter[ServerRequest] {
  //     override def get(carrier: ServerRequest, key: String): Option[String] =
  //       carrier.header(key)
  //     override def keys(carrier: ServerRequest): Iterable[String] =
  //       carrier.headers.map(_.name)
//}
  override def apply[R, B](
      responder: Responder[Task, B],
      requestHandler: EndpointInterceptor[Task] => RequestHandler[Task, R, B]
  ): RequestHandler[Task, R, B] =
    new RequestHandler[Task, R, B] {
      override def apply(
          request: ServerRequest,
          endpoints: List[ServerEndpoint[R, Task]]
      )(implicit monad: MonadError[Task]): Task[RequestResult[B]] = {

        config.tracing
          .extractSpanUnsafe(
            propagator,
            carrier,
            request.showShort,
            spanKind = SpanKind.SERVER
          )
          .flatMap: (span, finalize) =>
            (for {
              requestResult <- requestHandler(
                knownEndpointInterceptor(request, span)
              )(request, endpoints)
                .tapError { case e: Exception =>
                  ZIO
                    .succeed:
                      span.setStatus(StatusCode.ERROR)
                      span.setAttribute(
                        AttributeKey.stringKey("error.type"),
                        e.getClass.getName
                      )
                    .flatMap(_ => monad.error(e))
                }
              _ <- requestResult match {
                case Response(response, _) =>
                  ZIO
                    .succeed(
                      span
                        .setAllAttributes(
                          responseAttributes(request, response)
                        )
                    )
                    .flatMap(_ =>
                      if (response.isServerError)
                        ZIO.succeed {
                          span
                            .setStatus(
                              StatusCode.ERROR
                            )
                          span.setAllAttributes(
                            errorAttributes(Left(response.code))
                          )
                        } *> monad.error(
                          new Exception(
                            s"Server error with status code ${response.code}"
                          )
                        )
                      else monad.unit(())
                    )
                case Failure(_) =>
                  // ignore, request not handled
                  monad.unit(())
              }
            } yield requestResult)
              .ensuring(finalize)

        /*
      tracer.joinOrRoot(request)(
        tracer
          .spanBuilder(spanName(request))
          .addAttributes(requestAttributes(request))
          .withSpanKind(SpanKind.Server)
          .build
          .use(span =>
            (for {
              requestResult <- requestHandler(
                knownEndpointInterceptor(request, span)
              )(request, endpoints)
              _ <- requestResult match {
                case Response(response, _) =>
                  span
                    .addAttributes(responseAttributes(request, response))
                    .flatMap(_ =>
                      if (response.isServerError)
                        span
                          .setStatus(trace.StatusCode.Error)
                          .flatMap(_ =>
                            span.addAttributes(
                              errorAttributes(Left(response.code))
                            )
                          )
                      else monad.unit(())
                    )
                case Failure(_) =>
                  // ignore, request not handled
                  monad.unit(())
              }
            } yield requestResult)
              .handleError { case e: Exception =>
                span
                  .setStatus(trace.StatusCode.Error)
                  .flatMap(_ => span.addAttributes(errorAttributes(Right(e))))
                  .flatMap(_ => monad.error(e))
              }
          )
      )
    }
         */
      }

      def knownEndpointInterceptor(
          request: ServerRequest,
          span: Span
      ) =
        new EndpointInterceptor[Task] {
          def apply[B](
              responder: Responder[Task, B],
              endpointHandler: EndpointHandler[Task, B]
          ): EndpointHandler[Task, B] = new EndpointHandler[Task, B] {
            def onDecodeFailure(
                ctx: DecodeFailureContext
            )(implicit
                monad: MonadError[Task],
                bodyListener: BodyListener[Task, B]
            ): Task[Option[ServerResponse[B]]] =
              endpointHandler.onDecodeFailure(ctx).flatMap {
                case result @ Some(_) =>
                  knownEndpoint(ctx.endpoint).map(_ => result)
                case None => monad.unit(None)
              }

            def onDecodeSuccess[A, U, I](
                ctx: DecodeSuccessContext[Task, A, U, I]
            )(implicit
                monad: MonadError[Task],
                bodyListener: BodyListener[Task, B]
            ): Task[ServerResponse[B]] =
              knownEndpoint(ctx.endpoint).flatMap(_ =>
                endpointHandler.onDecodeSuccess(ctx)
              )

            def onSecurityFailure[A](
                ctx: SecurityFailureContext[Task, A]
            )(implicit
                monad: MonadError[Task],
                bodyListener: BodyListener[Task, B]
            ): Task[ServerResponse[B]] =
              knownEndpoint(ctx.endpoint).flatMap(_ =>
                endpointHandler.onSecurityFailure(ctx)
              )

            def knownEndpoint(
                e: AnyEndpoint
            ): Task[Unit] = {
              val (name, attributes) =
                spanNameFromEndpointAndAttributes(request, e)
              ZIO.succeed:
                span
                  .updateName(name)
                span.setAllAttributes(attributes)

            }
          }
        }
    }
}

object ZIOpenTelemetryTracing {
  def apply(
      tracing: Tracing
  ): ZIOpenTelemetryTracing =
    new ZIOpenTelemetryTracing(
      Otel4zTracingConfig(tracing),

      TraceContextPropagator.default,
      IncomingContextCarrier.default()
    )
}
