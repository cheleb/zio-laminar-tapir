package dev.cheleb.ziolaminartapir

import izumi.reflect.Tag

import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import zio.*
import sttp.model.Uri

trait DifferentOriginBackendClient {
  def endpointRequestZIO[I, E <: Throwable, O](
      baseUri: Option[Uri],
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O]
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
) extends DifferentOriginBackendClient {

  private def endpointRequest[I, E, O](
      baseUri: Option[Uri],
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, baseUri)

  /** Turn a secured endpoint into curried functions from Token => Input =>
    * Request.
    *
    * @param endpoint
    * @return
    */

  def endpointRequestZIO[I, E <: Throwable, O](
      baseUri: Option[Uri],
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): ZIO[Any, Throwable, O] =
    backend
      .send(endpointRequest(baseUri, endpoint)(payload))
      .map(_.body)
      .absolve

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
