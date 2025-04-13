package dev.cheleb.ziotapir

import zio.*
import zio.stream.*

import dev.cheleb.ziojwt.WithToken
import izumi.reflect.Tag
import sttp.capabilities.zio.ZioStreams
import sttp.client4.*
import sttp.client4.impl.zio.FetchZioBackend
import sttp.model.Uri
import sttp.tapir.Endpoint

import laminar.Session
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter

/** A client to the backend, extending the endpoints as methods.
  *
  * This client is used to call the backend from the frontend, but base URI must
  * be provided at each call.
  */
trait DifferentOriginBackendClient {

  /** Call an endpoint with a payload.
    *
    * @param baseUri
    *   the base URI of the backend
    * @param endpoint
    *   the endpoint to call
    * @param payload
    * @return
    */
  private[ziotapir] def requestZIO[I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O]

  /** Call a secured endpoint with a payload.
    *
    * Token will be taken from the session aka the user state aka the storage.
    *
    * Token are stored by issuer (the host and port of the issuer).
    *
    * @param baseUri
    *   the base URI of the backend
    * @param endpoint
    *   the endpoint to call
    * @param payload
    *   the payload of the request
    * @param session
    *   the session with the token
    * @return
    */
  private[ziotapir] def securedRequestZIO[
      UserToken <: WithToken,
      I,
      E <: Throwable,
      O
  ](
      baseUri: Uri,
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O]

  private[ziotapir] def streamRequestZIO[I, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I): Task[Stream[Throwable, O]]

  private[ziotapir] def securedStreamRequestZIO[
      UserToken <: WithToken,
      I,
      O
  ](
      baseUri: Uri,
      endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I)(using session: Session[UserToken]): Task[Stream[Throwable, O]]
}

/** The live implementation of the BackendClient with a different origin.
  *
  * @param backend
  *   the backend to use
  * @param interpreter
  *   the interpreter to use
  */
private class DifferentOriginBackendClientLive(
    backend: WebSocketStreamBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    streamInterpreter: StreamSttpClientInterpreter
) extends BackendClient(backend, interpreter, streamInterpreter)
    with DifferentOriginBackendClient {}

/** The live implementation of the BackendClient with a different origin.
  */
object DifferentOriginBackendClientLive {

  /** The layer to create the client from the backend and the interpreter.
    */
  private def layer =
    ZLayer.derive[DifferentOriginBackendClientLive]

  /** The layer to create the client.
    */
  private[ziotapir] def configuredLayer
      : ULayer[DifferentOriginBackendClient] = {
    val backend: WebSocketStreamBackend[Task, ZioStreams] =
      FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val streamInterpreter = StreamSttpClientInterpreter()

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      streamInterpreter
    ) >>> layer
  }

}
