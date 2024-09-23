package dev.cheleb.ziolaminartapir

import dev.cheleb.ziojwt.WithToken

import izumi.reflect.Tag

import org.scalajs.dom.window

import scala.scalajs.LinkingInfo
import scala.scalajs.js

import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import zio.*
import sttp.model.Uri

/** A client to the backend, extending the endpoints as methods.
  */
trait SameOriginBackendClient {

  /** Call an endpoint with a payload.
    *
    * This method turns an endpoint into a Task, that:
    *   - build a request from a payload
    *   - sends it to the backend
    *   - returns the response.
    *
    * @param endpoint
    * @param payload
    * @return
    */
  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O]

  /** Call a secured endpoint with a payload.
    *
    * This method turns a secured endpoint into a Task, that:
    *   - build a request from a payload and a security token
    *   - sends it to the backend
    *   - returns the response.
    *
    * @param endpoint
    * @param payload
    * @return
    */
  def securedEndpointRequestZIO[UserToken <: WithToken, I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): Task[O]
}

/** A client to the backend, extending the endpoints as methods.
  */

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private class SameOriginBackendClientLive(
    backend: SttpBackend[Task, ZioStreamsWithWebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient(backend, interpreter)
    with SameOriginBackendClient {

  /** Call an endpoint with a payload.
    * @param endpoint
    * @param payload
    * @return
    */
  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): ZIO[Any, Throwable, O] =
    endpointRequestZIO(config.baseUrl, endpoint)(payload)

    /** Call a secured endpoint with a payload.
      * @param endpoint
      * @param payload
      * @return
      */
  def securedEndpointRequestZIO[UserToken <: WithToken, I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O] =
    securedEndpointRequestZIO(config.baseUrl, endpoint)(payload)

}

/** The live implementation of the BackendClient.
  */
object SameOriginBackendClientLive {

  private def developmentApiServer =
    if js.typeOf(js.Dynamic.global.DEV_API_URL) == "string"
    then Uri.unsafeParse(js.Dynamic.global.DEV_API_URL.toString)
    else Uri.unsafeParse("http://localhost:8080")

  /** The layer that can be used to create a client.
    */
  private def layer: ZLayer[
    SttpBackend[Task, ZioStreamsWithWebSockets] &
      (SttpClientInterpreter & BackendClientConfig),
    Nothing,
    SameOriginBackendClientLive
  ] =
    ZLayer.derive[SameOriginBackendClientLive]

  val backendBaseURL =
    if LinkingInfo.developmentMode then developmentApiServer
    else Uri.unsafeParse(window.document.location.origin)

  def configuredLayer: ZLayer[Any, Nothing, SameOriginBackendClientLive] = {
    val backend: SttpBackend[Task, ZioStreamsWithWebSockets] = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config = BackendClientConfig(backendBaseURL)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

}
