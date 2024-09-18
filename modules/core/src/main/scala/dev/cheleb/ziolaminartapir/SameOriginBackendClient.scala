package dev.cheleb.ziolaminartapir

import dev.cheleb.ziojwt.WithToken

import izumi.reflect.Tag

import org.scalajs.dom.window

import scala.scalajs.LinkingInfo
import scala.scalajs.js

import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.capabilities.WebSockets

import zio.*
import sttp.model.Uri

type ZioStreamsWithWebSockets = ZioStreams & WebSockets

case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)

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

  /** Turn an endpoint into a function from Input => Request.
    *
    * @param endpoint
    * @return
    */
  private def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, config.baseUrlAsOption)

  /** Turn a secured endpoint into curried functions from Token => Input =>
    * Request.
    *
    * @param endpoint
    * @return
    */
  private def securedEndpointRequest[A, I, E, O](
      endpoint: Endpoint[A, I, E, O, Any]
  ): A => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(
      endpoint,
      config.baseUrlAsOption
    )

  override def isSameIssuer(token: WithToken): Option[Boolean] =
    for {
      configHost <- config.baseUrl.host
      confitPort <- config.baseUrl.port
    } yield token.issuer == s"${configHost}:${confitPort}"

  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): ZIO[Any, Throwable, O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve

  def securedEndpointRequestZIO[UserToken <: WithToken, I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O] =
    for {
      token <- tokenOfFail(None)
      res <- backend
        .send(securedEndpointRequest(endpoint)(token)(payload))
        .map(_.body)
        .absolve
    } yield res

}

object SameOriginBackendClientLive {

  def developmentApiServer =
    if js.typeOf(js.Dynamic.global.DEV_API_URL) == "string"
    then js.Dynamic.global.DEV_API_URL.toString
    else "http://localhost:8080"

  def layer: ZLayer[
    SttpBackend[Task, ZioStreamsWithWebSockets] &
      (SttpClientInterpreter & BackendClientConfig),
    Nothing,
    SameOriginBackendClientLive
  ] =
    ZLayer.derive[SameOriginBackendClientLive]

  def configurableLayer(baseUri: Uri): ZLayer[
    SttpBackend[Task, ZioStreamsWithWebSockets] & SttpClientInterpreter,
    Nothing,
    SameOriginBackendClientLive
  ] =
    ZLayer.succeed(BackendClientConfig(baseUri)) >>> ZLayer
      .derive[SameOriginBackendClientLive]

  val backendBaseURL =
    if LinkingInfo.developmentMode then
      Uri.unsafeParse(
        developmentApiServer
      )
    else Uri.unsafeParse(window.document.location.origin)

  def configuredLayer(
      baseURL: Uri
  ): ZLayer[Any, Nothing, SameOriginBackendClientLive] = {
    val backend: SttpBackend[Task, ZioStreamsWithWebSockets] = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config = BackendClientConfig(baseURL)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      config
    ) >>> layer
  }
  def configuredLayer: ZLayer[Any, Nothing, SameOriginBackendClientLive] = {
    val backend: SttpBackend[Task, ZioStreamsWithWebSockets] = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val config = BackendClientConfig(backendBaseURL)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

}
