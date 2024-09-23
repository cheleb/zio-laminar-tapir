package dev.cheleb.ziolaminartapir

import zio.*

import scala.annotation.targetName

import com.raquo.laminar.api.L.*
import sttp.tapir.Endpoint
import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

/** Extension methods for ZIO JS.
  *
  * Extension are of two kinds:
  *   - targeting Endpoints: allowing to call an endpoint with a payload
  *   - targeting ZIO: allowing to run a ZIO in JS
  *
  * Also, there are two kinds of ZIO, and associated 2 extensions:
  *   - SameOriginBackendClient: for requests to the same origin
  *   - DifferentOriginBackendClient: for requests to a different origin
  */

type ZioStreamsWithWebSockets = ZioStreams & WebSockets

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)

/** ZIO JS extension methods.
  *
  * This object contains:
  *   - convenience methods for calling endpoints.
  *   - extension methods for ZIO that are specific to the Laminar JS
  *     environment.
  */

/** Extension to ZIO[SameOriginBackendClient, E, A] that allows us to run in JS.
  */
extension [E <: Throwable, A](zio: ZIO[SameOriginBackendClient, E, A])

  /** Run the underlying request to the default backend.
    */
  private def exec: Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(SameOriginBackendClientLive.configuredLayer)
      )
    }

  /** Run the ZIO in JS.
    *
    * @return
    */
  def runJs: Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec

  /** Run the ZIO in JS, and emit the error to an EventBus.
    * @param errorBus
    *   the event bus to emit the error to
    */
  def runJs(errorBus: EventBus[E]): Unit =
    zio
      .tapError(e => ZIO.attempt(errorBus.emit(e)))
      .exec

  /** Emit the result of the ZIO to an EventBus.
    *
    * Underlying request to the default backend.
    * @param bus
    */
  def emitTo(bus: EventBus[A]): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Emit the result and error of the ZIO to an EventBus.
    *
    * Underlying request to the default backend.
    *
    * @param bus
    * @param error
    */
  def emitTo(
      bus: EventBus[A],
      error: EventBus[E]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(error.emit(e)))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Emit the result of the ZIO to an EventBus of Either.
    *
    * Underlying request to the default backend.
    *
    * @param bus
    *   event bus to emit the result to
    */
  def emitToEither(
      bus: EventBus[Either[E, A]]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(bus.emit(Left(e))))
      .tap(a => ZIO.attempt(bus.emit(Right(a))))
      .exec

  /** Emit the result of the ZIO to an EventBus, and return the EventStream.
    */
  def toEventStream: EventStream[A] = {
    val eventBus = EventBus[A]()
    emitTo(eventBus)
    eventBus.events
  }

/** Extension to ZIO[DifferentOriginBackendClient, E, A] that allows us to run
  * in JS.
  */
extension [E <: Throwable, A](zio: ZIO[DifferentOriginBackendClient, E, A])
  /** Run the underlying request to the default backend.
    */
  @targetName("dexec")
  private def exec: Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(DifferentOriginBackendClientLive.configuredLayer)
      )
    }

  /** Run the ZIO in JS.
    *
    * @return
    */
  @targetName("drunJs")
  def runJs: Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec

  /** Run the ZIO in JS, and emit the error to an EventBus.
    * @param errorBus
    *   the event bus to emit the error to
    */
  @targetName("drunJs")
  def runJs(errorBus: EventBus[E]): Unit =
    zio
      .tapError(e => ZIO.attempt(errorBus.emit(e)))
      .exec

  /** Emit the result of the ZIO to an EventBus.
    *
    * Underlying request to the default backend.
    * @param bus
    */
  @targetName("demitTo")
  def emitTo(bus: EventBus[A]): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Emit the result and error of the ZIO to an EventBus.
    *
    * Underlying request to the default backend.
    *
    * @param bus
    * @param error
    */
  @targetName("demitTo")
  def emitTo(
      bus: EventBus[A],
      error: EventBus[E]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(error.emit(e)))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Emit the result of the ZIO to an EventBus of Either.
    *
    * Underlying request to the default backend.
    *
    * @param bus
    *   event bus to emit the result to
    */
  @targetName("demitToEither")
  def emitToEither(
      bus: EventBus[Either[E, A]]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(bus.emit(Left(e))))
      .tap(a => ZIO.attempt(bus.emit(Right(a))))
      .exec

  /** Emit the result of the ZIO to an EventBus, and return the EventStream.
    */
  @targetName("dtoEventStream")
  def toEventStream: EventStream[A] = {
    val eventBus = EventBus[A]()
    emitTo(eventBus)
    eventBus.events
  }

/** Extension that allows us to turn an unsecure endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, E <: Throwable, O](
    endpoint: Endpoint[Unit, I, E, O, Any]
)
  /** Call the endpoint with a payload, and get a ZIO back.
    * @param payload
    * @return
    */
  def apply(payload: I): RIO[SameOriginBackendClient, O] =
    ZIO.debug(payload.toString()) *>
      ZIO
        .service[SameOriginBackendClient]
        .flatMap(_.endpointRequestZIO(endpoint)(payload))

  /** Call the endpoint with a payload on a different backend than the origin,
    * and get a ZIO back.
    */
  @targetName("dapply")
  def on(baseUri: Uri)(payload: I): RIO[DifferentOriginBackendClient, O] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.endpointRequestZIO(baseUri, endpoint)(payload))

/** Extension that allows us to turn a secured endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])
  /** Call the secured endpoint with a payload, and get a ZIO back.
    * @param payload
    * @return
    */
  @targetName("securedApply")
  def apply[UserToken <: WithToken](
      payload: I
  )(using session: Session[UserToken]): RIO[SameOriginBackendClient, O] =
    ZIO
      .service[SameOriginBackendClient]
      .flatMap(_.securedEndpointRequestZIO(endpoint)(payload))

  /** Call the secured endpoint with a payload on a different backend than the
    * origin, and get a ZIO back.
    */
  def on[UserToken <: WithToken](baseUri: Uri)(
      payload: I
  )(using session: Session[UserToken]): RIO[DifferentOriginBackendClient, O] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.securedEndpointRequestZIO(baseUri, endpoint)(payload))
