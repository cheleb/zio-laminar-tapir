package dev.cheleb.ziotapir

import zio.*
import zio.stream.*

import scala.annotation.targetName

import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.*
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client4.Response
import sttp.tapir.Endpoint
import sttp.ws.WebSocketFrame

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)

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
import sttp.tapir.client.sttp4.ws.zio.* // for zio

extension [I, E, WI, WO](
    wse: Endpoint[
      Unit,
      I,
      E,
      ZioStreams.Pipe[WI, WO],
      ZioStreams & WebSockets
    ]
) // (using WebSocketToPipe[ZioStreams & WebSockets])
  /** Call the WebSocket endpoint with a payload, and get a ZIO back.
    */
  def responseZIO(payload: I): RIO[BackendClient, Response[
    ZioStreams.Pipe[WI, WO]
  ]] = for {
    backendClient <- ZIO.service[BackendClient]
    response <- backendClient
      .wsResponseZIO(wse)(payload)
      .tapError(th =>
        Console.printLineError(
          s"WebSocket connection failed: ${th.getMessage()}"
        )
      )
  } yield response

  @targetName("wsApply")
  def apply(payload: I): RIO[
    BackendClient,
    ZioStreams.Pipe[WI, WO]
  ] =
    for {
      backendClient <- ZIO.service[BackendClient]
      client <- backendClient
        .wsClientZIO(wse)(payload)
        .tapError(th =>
          Console.printLineError(
            s"WebSocket connection failed: ${th.getMessage()}"
          )
        )
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

/** Extensions for `Hub[WebSocketFrame]`.
  */
extension (hub: Hub[WebSocketFrame])

  /** Sends a text message over the WebSocket.
    *
    * @param message
    * @return
    *   A `UIO[Boolean]` indicating whether the message was successfully
    *   published.
    */
  def sendText(message: String): UIO[Boolean] =
    hub.publish(WebSocketFrame.text(message))

  /** Closes the WebSocket connection gracefully by sending a Close frame and
    * shutting down the hub.
    *
    * @return
    *   A `UIO[Unit]` that completes when the close operation is done.
    */
  def closeGracefully: UIO[Unit] =
    ZIO.uninterruptible:
      for
        _ <- hub.publish(WebSocketFrame.Close(1000, "Normal Closure"))
        _ <- hub.shutdown
      yield ()
