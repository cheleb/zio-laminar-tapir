package dev.cheleb.ziolaminartapir

import zio.*

import scala.annotation.targetName

import com.raquo.laminar.api.L.*
import sttp.tapir.Endpoint
import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri

/** ZIO JS extension methods.
  *
  * This object contains:
  *   - convenience methods for calling endpoints.
  *   - extension methods for ZIO that are specific to the Laminar JS
  *     environment.
  */

/** Extension to ZIO[BakendClient, E, A] that allows us to run in JS.
  *
  * This is a side effect, and should be used with caution.
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
extension [E <: Throwable, A](zio: ZIO[SameOriginBackendClient, E, A])

  /** Run the underlying request to the default backend.
    */

  /** Run the underlying request to the default backend.
    */
  private def exec: Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(SameOriginBackendClientLive.configuredLayer)
      )
    }

  /** Run the underlying request to the backend at uri.
    */
  private def exec(uri: Uri): Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(SameOriginBackendClientLive.configuredLayer(uri))
      )
    }

  /** Emit the result of the ZIO to an EventBus.
    *
    * @param baseURL
    *   The base URL to send the request to.
    * @param bus
    *   The EventBus to emit the result to.
    */
  def emitTo(baseURL: Uri, bus: EventBus[A]): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec(baseURL)

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

  /** Emit the result and error of the ZIO to an EventBus.
    *
    * @param baseURL
    *   uri to send the request to
    * @param bus
    *   event bus to emit the result to
    * @param error
    *   event bus to emit the error to
    */
  def emitTo(
      baseURL: Uri,
      bus: EventBus[A],
      error: EventBus[E]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(error.emit(e)))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec(baseURL)

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

  /** Run the ZIO in JS.
    *
    * emits the error to the errorBus
    *
    * @param baseURL
    *   the base URL to send the request to
    * @param errorBus
    */
  def runJs(baseURL: Uri, errorBus: EventBus[E]): Unit =
    zio
      .tapError(e => ZIO.attempt(errorBus.emit(e)))
      .exec(baseURL)

  /** Run the ZIO in JS.
    *
    * @return
    */
  def runJs: Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec

  /** Emit the result of the ZIO to an EventBus, and return the EventStream.
    *
    * @return
    */
  def toEventStream(baseURL: Uri): EventStream[A] = {
    val eventBus = EventBus[A]()
    emitTo(baseURL, eventBus)
    eventBus.events
  }

  def toEventStream: EventStream[A] = {
    val eventBus = EventBus[A]()
    emitTo(eventBus)
    eventBus.events
  }

extension [E <: Throwable, A](zio: ZIO[DifferentOriginBackendClient, E, A])
  /** Run the ZIO in JS.
    *
    * emits the error to the console
    *
    * @param baseURL
    *   the base URL to send the request to
    *
    * @return
    */
  @targetName("drunJs")
  def runJs: Unit =
    zio
      .tap(a => Console.printLine(a.toString()))
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec

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

  def on[UserToken <: WithToken](baseUri: Uri)(
      payload: I
  )(using session: Session[UserToken]): RIO[DifferentOriginBackendClient, O] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.securedEndpointRequestZIO(baseUri, endpoint)(payload))
