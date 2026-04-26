package dev.cheleb.ziotapir

import zio.*

import scala.scalajs.LinkingInfo
import scala.scalajs.js

import dev.cheleb.ziotapir.*
import izumi.reflect.Tag
import org.scalajs.dom.window
import sttp.client4.*
import sttp.client4.impl.zio.FetchZioBackend
import sttp.model.Uri
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter
import sttp.tapir.client.sttp4.ws.WebSocketSttpClientInterpreter

/** The live implementation of the BackendClient.
  */
object FetchBackendClientLive {

  /** The base URL of the backend in development mode. It is the value of the
    */
  private def developmentApiServer =
    if js.typeOf(js.Dynamic.global.DEV_API_URL) == "string"
    then Uri.unsafeParse(js.Dynamic.global.DEV_API_URL.toString)
    else Uri.unsafeParse("http://localhost:8080")

  /** The base URL of the backend. It is the origin of the current page in
    * production mode, and localhost:8080 or the DEV_API_URL environment
    * variable in development mode.
    * {{{
    * DEV_API_URL ="http://localhost:9999/";
    * }}}
    */
  lazy val backendBaseURL =
    if LinkingInfo.developmentMode then developmentApiServer
    else Uri.unsafeParse(window.document.location.origin)

  /** The base URL of the frontend.
    */
  lazy val frontendBaseURL =
    Uri.unsafeParse(window.document.location.origin)

  /** Convenience method to create a URL for the backend.
    *
    * The base URL of the backend.
    */
  def url(paths: String*): Uri =
    backendBaseURL.addPath(paths)

  /** Convenience method to create a URL for the frontend.
    */
  def resourceUrl(paths: String*): Uri =
    frontendBaseURL.addPath(paths)

    /** The layer that can be used to create a client.
      */
  private def layer =
    ZLayer.derive[BackendClientLive]

  /** The layer that can be used to create
    */
  private[ziotapir] def configuredLayer
      : ZLayer[Any, Nothing, BackendClientLive] = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val streamInterpreter = StreamSttpClientInterpreter()
    val websocketInterpreter = WebSocketSttpClientInterpreter()
    val config = BackendClientConfig(backendBaseURL)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      streamInterpreter
    ) ++ ZLayer.succeed(
      websocketInterpreter
    ) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

  private[ziotapir] def configuredLayerOn(
      uri: Uri
  ): ZLayer[Any, Nothing, BackendClientLive] = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val streamInterpreter = StreamSttpClientInterpreter()
    val websocketInterpreter = WebSocketSttpClientInterpreter()
    val config = BackendClientConfig(uri)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      streamInterpreter
    ) ++ ZLayer.succeed(websocketInterpreter) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

}
