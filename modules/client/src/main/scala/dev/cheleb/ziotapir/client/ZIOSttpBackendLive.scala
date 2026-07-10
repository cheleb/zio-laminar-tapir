package dev.cheleb.ziotapir.client

import zio.*

import sttp.client4.*
import dev.cheleb.ziotapir.*
import sttp.tapir.client.sttp4.*
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter
import sttp.tapir.client.sttp4.ws.WebSocketSttpClientInterpreter
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import sttp.capabilities.zio.ZioStreams

object ZIOSttpBackendLive {

  /** The layer that can be used to create a client.
    */
  private def layer: URLayer[WebSocketStreamBackend[Task, ZioStreams] & SttpClientInterpreter & StreamSttpClientInterpreter & WebSocketSttpClientInterpreter & BackendClientConfig, BackendClient] =
    ZLayer.derive[BackendClientLive]

  def configuredLayer: TaskLayer[BackendClient] = 
    configuredLayerOn(Uri.unsafeParse("http://localhost:8080"))

  def configuredLayerOn(
      uri: Uri
  ): TaskLayer[BackendClient] = {
    val backend = HttpClientZioBackend.layer()
    val interpreter = SttpClientInterpreter()
    val streamInterpreter = StreamSttpClientInterpreter()
    val websocketInterpreter = WebSocketSttpClientInterpreter()
    val config = BackendClientConfig(uri)

    backend ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      streamInterpreter
    ) ++ ZLayer.succeed(websocketInterpreter) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

}
