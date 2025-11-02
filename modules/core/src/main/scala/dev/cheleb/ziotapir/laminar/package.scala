/** Extension methods for ZIO JS.
  * ==Overview==
  *
  * Extension are of two kinds:
  *   - targeting Endpoints: allowing to call an endpoint with a payload
  *   - targeting ZIO: allowing to run a ZIO in JS
  *
  * This object contains:
  *   - convenience methods for calling endpoints.
  *   - extension methods for ZIO that are specific to the Laminar JS
  *     environment.
  */

package dev.cheleb.ziotapir.laminar

import zio.*
import zio.json.*
import zio.stream.*

import scala.annotation.targetName

import com.raquo.laminar.api.L.*
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.*
import sttp.capabilities.zio.ZioStreams
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.capabilities.WebSockets
import sttp.client4.Response
import sttp.tapir.client.sttp4.WebSocketToPipe

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
  def apply(payload: I): RIO[BackendClient, O] =
    ZIO
      .service[BackendClient]
      .flatMap(_.requestZIO(endpoint)(payload))

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
  )(using session: Session[UserToken]): RIO[BackendClient, O] =
    ZIO
      .service[BackendClient]
      .flatMap(_.securedRequestZIO(endpoint)(payload))

/** Extension that allows us to turn a stream endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, O](
    endpoint: Endpoint[Unit, I, Throwable, Stream[Throwable, O], ZioStreams]
)

  /** Call the endpoint with a payload, and get a ZIO back.
    */
  @targetName("streamApply")
  def apply(payload: I): RIO[BackendClient, Stream[Throwable, O]] =
    ZIO
      .service[BackendClient]
      .flatMap(_.streamRequestZIO(endpoint)(payload))

/** Extension that allows us to turn a stream endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, O](
    endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
)

  /** Call the secured stream endpoint with a payload, and get a ZIO back.
    */
  @targetName("securedStreamApply")
  def apply[UserToken <: WithToken](payload: I)(using
      session: Session[UserToken]
  ): RIO[BackendClient, Stream[Throwable, O]] =
    ZIO
      .service[BackendClient]
      .flatMap(_.securedStreamRequestZIO(endpoint)(payload))

/** WebSocket extension methods.
  */
extension [I, E, WI, WO](
    wse: Endpoint[
      Unit,
      I,
      E,
      ZioStreams.Pipe[WI, WO],
      ZioStreams & WebSockets
    ]
)(using WebSocketToPipe[ZioStreams & WebSockets])
  @targetName("wsApply")
  def applyTT(payload: I): ZIO[BackendClient, Throwable, Response[
    ZStream[Any, Throwable, WI] => ZStream[Any, Throwable, WO]
  ]] = for {
    backendClient <- ZIO.service[BackendClient]
    client <- backendClient.wsClientZIO(wse)(payload)
  } yield client

/** Extension to ZIO[Any, E, A] that allows us to run in JS.
  *
  * @param zio
  *   the ZIO to run
  */
extension [E <: Throwable, A](zio: ZIO[Any, E, A])

  def runSyncUnsafe(): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe
        .run(
          zio
        )
        .getOrThrow()
    }

/** Extension to ZIO[BackendClient, E, A] that allows us to run in JS.
  *
  * @param zio
  *   the ZIO to run
  */
extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A])

  /** Run the underlying request to the same origin backend.
    */
  private def exec: Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(BackendClientLive.configuredLayer)
      )
    }

  /** Run the underlying request to the different origin backend.
    * @param uri
    *   the URI to run the request on
    */

  private def exec(uri: Uri): Unit =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        zio.provide(BackendClientLive.configuredLayerOn(uri))
      )
    }

  /** Run the ZIO in JS and print the error to the console.
    *
    * This method is useful for debugging, as it will print the error message to
    * the console if the ZIO fails.
    *
    * @return
    */
  def run: Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec

  /** Run the ZIO in JS and print the error to the console.
    *
    * This method is useful for debugging, as it will print the error message to
    * the console if the ZIO fails. *
    * @return
    */
  def run(uri: Uri): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .exec(uri)

  /** Run the ZIO in JS, and emit the error to an EventBus.
    * @param errorBus
    *   the event bus to emit the error to
    */
  def run(errorBus: EventBus[E]): Unit =
    zio
      .tapError(e => ZIO.attempt(errorBus.emit(e)))
      .exec

  /** Run the ZIO in JS on a specific URI, and emit the error to an EventBus.
    * @param uri
    *   the URI to run the request on
    * @param errorBus
    *   the event bus to emit the error to
    */
  def run(uri: Uri, errorBus: EventBus[E]): Unit =
    zio
      .tapError(e => ZIO.attempt(errorBus.emit(e)))
      .exec(uri)

  /** Run zio on same origin and emit the result to an EventBus.
    *
    * Prints the error to the console if the ZIO fails.
    * @param bus
    *   event bus to emit the result to
    * @tparam A
    *   the type of the result
    */
  def emit(bus: EventBus[A]): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Run zio on different origin and emit the result to an EventBus.
    *
    * Prints the error to the console if the ZIO fails.
    * @param bus
    *   event bus to emit the result to
    * @tparam A
    *   the type of the result
    */
  def emit(uri: Uri, bus: EventBus[A]): Unit =
    zio
      .tapError(th => Console.printLineError(th.getMessage()))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec(uri)

  /** Run zio on same origin and emit the result and error of the ZIO to an
    * EventBus.
    *
    * @param bus
    *   event bus to emit the result to
    * @param error
    *   event bus to emit the error to
    */
  def emit(
      bus: EventBus[A],
      error: EventBus[E]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(error.emit(e)))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec

  /** Run zio on different origin and emit the result and error of the ZIO to an
    * EventBus.
    *
    * @param bus
    *   event bus to emit the result to
    * @param error
    *   event bus to emit the error to
    */
  def emit(
      uri: Uri,
      bus: EventBus[A],
      error: EventBus[E]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(error.emit(e)))
      .tap(a => ZIO.attempt(bus.emit(a)))
      .exec(uri)

  /** Run zio on same origin and emit the result of the ZIO to an EventBus of
    * Either.
    *
    * Underlying request to the default backend.
    *
    * @param bus
    *   event bus to emit the result to
    */
  def emitEither(
      bus: EventBus[Either[E, A]]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(bus.emit(Left(e))))
      .tap(a => ZIO.attempt(bus.emit(Right(a))))
      .exec

  /** Run zio on different origin and emit the result of the ZIO to an EventBus
    * of Either.
    *
    * Underlying request to the default backend.
    *
    * @param uri
    *   the URI to run the request on
    * @param bus
    *   event bus to emit the result to
    * @tparam A
    *   the type of the result
    * @tparam E
    *   the type of the error
    */
  def emitEither(
      uri: Uri,
      bus: EventBus[Either[E, A]]
  ): Unit =
    zio
      .tapError(e => ZIO.attempt(bus.emit(Left(e))))
      .tap(a => ZIO.attempt(bus.emit(Right(a))))
      .exec(uri)

  /** Emit the result of the ZIO to an EventBus, and return the EventStream.
    */
  def toEventStream: EventStream[A] = {
    val eventBus = EventBus[A]()
    emit(eventBus)
    eventBus.events
  }

  /** Emit the result of the ZIO to an EventBus, and return the EventStream.
    */
  def toEventStream(uri: Uri): EventStream[A] = {
    val eventBus = EventBus[A]()
    emit(uri, eventBus)
    eventBus.events
  }

extension (stream: ZStream[Any, Throwable, Byte])
  def parse[O: JsonCodec]: ZStream[Any, Throwable, Either[String, O]] =
    stream
      .via(ZPipeline.utf8Decode)
      .via(ZPipeline.splitLines)
      .via(ZPipeline.map(_.fromJson[O]))

/** Extension methods for ZIO[BackendClient, Throwable, ZStream], that parse
  * JSONL.
  */
extension (
    zio: ZIO[
      BackendClient,
      Throwable,
      ZStream[Any, Throwable, Byte]
    ]
)
  /** Parse a JSONL stream as a stream of Either[String, O] and apply a function
    * to each one to Task[Unit].
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlEitherZIO[O: JsonCodec](f: Either[String, O] => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream.parse
          .runForeach(f)
      )
      .run

  /** Parse a JSONL stream as a stream of Either[String, O] and apply a function
    * to each one to Task[Unit].
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlEitherZIO[O: JsonCodec](
      uri: Uri
  )(f: Either[String, O] => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream.parse
          .runForeach(f)
      )
      .run(uri)

  /** Parse a JSONL stream as a stream of O and apply a function to each one to
    * Task[Unit].
    *
    * Simply ignores the JSON parsing errors.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlZIO[O: JsonCodec](f: O => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runForeach(f)
      )
      .run

  /** Parse a JSONL stream from a URI (different from origin) as a stream of O
    * and apply a function to each one to Task[Unit].
    *
    * Simply ignores the JSON parsing errors.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlZIO[O: JsonCodec](uri: Uri)(f: O => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runForeach(f)
      )
      .run(uri)

  /** Parse a JSONL stream as a stream of Either[String, O] and apply a function
    * to each one to Unit.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlEither[O: JsonCodec](f: Either[String, O] => Unit) =
    zio
      .flatMap(stream =>
        stream.parse
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .run

  /** Parse a JSONL stream from a URI (different from origin) as a stream of O
    * and apply a function to each one to Unit.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonl[O: JsonCodec](uri: Uri)(f: O => Unit) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .run(uri)

  /** Parse a JSONL stream as a stream of O and apply a function to each one to
    * Unit.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonl[O: JsonCodec](f: O => Unit) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .run

  /** Parse a JSONL stream from a URI (different from origin) as a stream of
    * Either[String, O] and apply a function to each one to Unit.
    *
    * @param f
    *   function to apply
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlEither[O: JsonCodec](uri: Uri)(f: Either[String, O] => Unit) =
    zio
      .flatMap(stream =>
        stream.parse
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .run(uri)

  /** Parse a JSONL stream and fold it over a state.
    *
    * @param s
    *   initial state to fold over the stream
    * @param f
    *   function to apply
    * @tparam S
    *   the type of the state to fold over the stream
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    *
    * @return
    *   a ZIO that returns the final state after folding over the stream.
    */
  def jsonlFoldEitherZIO[S, O: JsonCodec](
      s: S
  )(f: (S, Either[String, O]) => Task[S]) =
    zio
      .flatMap(stream =>
        stream.parse
          .runFoldZIO(s)(f)
      )
      .run

  /** Parse a JSONL stream from a URI (different from origin) and fold it over a
    * state.
    *
    * @param uri
    *   the URI to run the request on
    * @param s
    *   initial state to fold over the stream
    * @param f
    *   function to apply
    * @tparam S
    *   the type of the state to fold over the stream
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    *
    * @return
    *   a ZIO that returns the final state after folding over the stream.
    */
  def jsonlFoldEitherZIO[S, O: JsonCodec](uri: Uri, s: S)(
      f: (S, Either[String, O]) => Task[S]
  ) =
    zio
      .flatMap(stream =>
        stream.parse
          .runFoldZIO(s)(f)
      )
      .run(uri)

  /** Parse a JSONL stream and fold it over a state.
    *
    * @param s
    *   initial state to fold over the stream
    * @param f
    *   function to apply
    * @tparam S
    *   the type of the state to fold over the stream
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    *
    * @return
    *   a ZIO that returns the final state after folding over the stream.
    */
  def jsonlFoldZIO[S, O: JsonCodec](
      s: S
  )(f: (S, O) => Task[S]) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runFoldZIO(s)(f)
      )
      .run

  /** Parse a JSONL stream from a URI (different from origin) and fold it over a
    * state.
    *
    * @param uri
    *   the URI to run the request on
    * @param s
    *   initial state to fold over the stream
    * @param f
    *   function to apply
    * @tparam S
    *   the type of the state to fold over the stream
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    *
    * @return
    *   a ZIO that returns the final state after folding over the stream.
    */
  def jsonlFoldZIO[S, O: JsonCodec](uri: Uri, s: S)(
      f: (S, O) => Task[S]
  ) =
    zio
      .flatMap(stream =>
        stream.parse.collectRight
          .runFoldZIO(s)(f)
      )
      .run(uri)
