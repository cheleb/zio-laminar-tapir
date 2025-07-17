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

/** Extension methods for ZIO JS.
  *
  * Extension are of two kinds:
  *   - targeting Endpoints: allowing to call an endpoint with a payload
  *   - targeting ZIO: allowing to run a ZIO in JS
  *
  * Also, there are two kinds of ZIO, and associated 2 extensions:
  *   - SameOriginBackendClient: for requests to the same origin
  *   - DifferentOriginBackendClient: for requests to a different origin
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
    ZIO
      .service[SameOriginBackendClient]
      .flatMap(_.requestZIO(endpoint)(payload))

  /** Call the endpoint with a payload on a different backend than the origin,
    * and get a ZIO back.
    */
  @targetName("dapply")
  def on(baseUri: Uri)(payload: I): RIO[DifferentOriginBackendClient, O] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.requestZIO(baseUri, endpoint)(payload))

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
      .flatMap(_.securedRequestZIO(endpoint)(payload))

  /** Call the secured endpoint with a payload on a different backend than the
    * origin, and get a ZIO back.
    */
  def on[UserToken <: WithToken](baseUri: Uri)(
      payload: I
  )(using session: Session[UserToken]): RIO[DifferentOriginBackendClient, O] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.securedRequestZIO(baseUri, endpoint)(payload))

/** Extension that allows us to turn a stream endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, O](
    endpoint: Endpoint[Unit, I, Throwable, Stream[Throwable, O], ZioStreams]
)

  /** Call the endpoint with a payload on a different backend than the origin,
    * and get a ZIO back.
    */
  @targetName("streamOn")
  def on(
      baseUri: Uri
  )(payload: I): RIO[DifferentOriginBackendClient, Stream[Throwable, O]] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.streamRequestZIO(baseUri, endpoint)(payload))

    /** Call the endpoint with a payload, and get a ZIO back.
      */
  @targetName("streamApply")
  def apply(payload: I): RIO[SameOriginBackendClient, Stream[Throwable, O]] =
    ZIO
      .service[SameOriginBackendClient]
      .flatMap(_.streamRequestZIO(endpoint)(payload))

/** Extension that allows us to turn a stream endpoint to a function from a
  * payload to a ZIO.
  */
extension [I, O](
    endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
)

  /** Call the secured stream endpoint with a payload on a different backend
    * than the origin, and get a ZIO back.
    */
  @targetName("securedStreamOn")
  def on[UserToken <: WithToken](
      baseUri: Uri
  )(payload: I)(using
      session: Session[UserToken]
  ): RIO[DifferentOriginBackendClient, Stream[Throwable, O]] =
    ZIO
      .service[DifferentOriginBackendClient]
      .flatMap(_.securedStreamRequestZIO(baseUri, endpoint)(payload))

  /** Call the secured stream endpoint with a payload, and get a ZIO back.
    */
  @targetName("securedStreamApply")
  def apply[UserToken <: WithToken](payload: I)(using
      session: Session[UserToken]
  ): RIO[SameOriginBackendClient, Stream[Throwable, O]] =
    ZIO
      .service[SameOriginBackendClient]
      .flatMap(_.securedStreamRequestZIO(endpoint)(payload))

/** Extension methods for ZIO[SameOriginBackendClient, Throwable, ZStream], that
  * parse JSONL.
  */
extension (
    zio: ZIO[
      SameOriginBackendClient,
      Throwable,
      ZStream[Any, Throwable, Byte]
    ]
)
  /** Parse a JSONL stream.
    * @param f
    *   function to apply to each parsed JSON object, which is an Either[String,
    *   O] where O is the parsed object, and String is an error message if
    *   parsing failed.
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlZIO[O: JsonCodec](f: Either[String, O] => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .runForeach(f)
      )
      .runJs

  /** Parse a JSONL stream.
    * @param f
    *   function to apply to each parsed JSON object, which is an Either[String,
    *   O] where O is the parsed object, and String is an error message if
    *   parsing failed.
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonlZIOSuccess[O: JsonCodec](f: O => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .collectRight
          .runForeach(f)
      )
      .runJs

  /** Parse a JSONL stream.
    * @param f
    *   function to apply to each parsed JSON object, which is an Either[String,
    *   O] where O is the parsed object, and String is an error message if
    *   parsing failed.
    * @tparam O
    *   the type of the parsed object, which must have a JsonCodec instance
    *   available.
    */
  def jsonl[O: JsonCodec](f: Either[String, O] => Unit) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .runJs

  /** Parse a JSONL stream and fold it over a state.
    *
    * This method allows you to fold over the stream with an initial state and a
    * function that processes each parsed JSON object.
    *
    * This is useful when you want to accumulate results or maintain a state,
    * like a cache.
    *
    * The stream is parsed line by line, and each line is parsed as a JSON
    * object. If a line cannot be parsed, it is skipped, and the error message
    * is returned as a Left in the Either. The final result is a ZIO that
    * returns the final state after folding over the stream.
    *
    * @param s
    *   initial state to fold over the stream
    * @param f
    *   function to apply to each parsed JSON object, which is an Either[String,
    *   O] where O is the parsed object, and String is an error message if
    *   parsing failed.
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
  )(f: (S, Either[String, O]) => Task[S]) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .runFoldZIO(s)(f)
      )
      .runJs

/** Extension methods for ZIO[DifferentOriginBackendClient, Throwable, ZStream],
  * that parse JSONL.
  */
extension (
    zio: ZIO[
      DifferentOriginBackendClient,
      Throwable,
      ZStream[Any, Throwable, Byte]
    ]
)
  /** Parse a JSONL stream.
    */
  @targetName("djsonlZIO")
  def jsonlZIO[O: JsonCodec](f: Either[String, O] => Task[Unit]) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .runForeach(f)
      )
      .runJs

  /** Parse a JSONL stream.
    */
  @targetName("djsonl")
  def jsonl[O: JsonCodec](f: Either[String, O] => Unit) =
    zio
      .flatMap(stream =>
        stream
          .via(ZPipeline.utf8Decode)
          .via(ZPipeline.splitLines)
          .via(ZPipeline.map(_.fromJson[O]))
          .runForeach(o => ZIO.attempt(f(o)))
      )
      .runJs
