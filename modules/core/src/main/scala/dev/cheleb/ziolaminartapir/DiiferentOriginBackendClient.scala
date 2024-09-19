package dev.cheleb.ziolaminartapir

import izumi.reflect.Tag

import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import zio.*
import sttp.model.Uri
import dev.cheleb.ziojwt.WithToken

trait DifferentOriginBackendClient {
  def endpointRequestZIO[I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O]
  def securedEndpointRequestZIO[UserToken <: WithToken, I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O]

}

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private class DifferentOriginBackendClientLive(
    backend: SttpBackend[Task, ZioStreamsWithWebSockets],
    interpreter: SttpClientInterpreter
) extends BackendClient(backend, interpreter)
    with DifferentOriginBackendClient {

  def isSameIssuer(token: WithToken): Boolean = true

}

object DifferentOriginBackendClientLive {

  def layer: ZLayer[
    SttpBackend[Task, ZioStreamsWithWebSockets] & (SttpClientInterpreter),
    Nothing,
    DifferentOriginBackendClient
  ] =
    ZLayer.derive[DifferentOriginBackendClientLive]

  def configuredLayer: ZLayer[Any, Nothing, DifferentOriginBackendClient] = {
    val backend: SttpBackend[Task, ZioStreamsWithWebSockets] = FetchZioBackend()
    val interpreter = SttpClientInterpreter()

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) >>> layer
  }

}
