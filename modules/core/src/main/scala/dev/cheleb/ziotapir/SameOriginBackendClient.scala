package dev.cheleb.ziotapir

import zio.*
import zio.stream.*

import scala.scalajs.LinkingInfo
import scala.scalajs.js

import dev.cheleb.ziojwt.WithToken
import izumi.reflect.Tag
import org.scalajs.dom.window
import sttp.capabilities.zio.ZioStreams
import sttp.client4.*
import sttp.client4.impl.zio.FetchZioBackend
import sttp.model.Uri
import sttp.tapir.Endpoint

import laminar.Session
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter

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
  private[ziotapir] def requestZIO[I, E <: Throwable, O](
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
  private[ziotapir] def securedRequestZIO[
      UserToken <: WithToken,
      I,
      E <: Throwable,
      O
  ](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): Task[O]

  private[ziotapir] def streamRequestZIO[I, O](
      endpoint: Endpoint[Unit, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I): Task[Stream[Throwable, O]]

  private[ziotapir] def securedStreamRequestZIO[UserToken <: WithToken, I, O](
      endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I)(using session: Session[UserToken]): Task[Stream[Throwable, O]]

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
    backend: WebSocketStreamBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    streamInterpreter: StreamSttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient(backend, interpreter, streamInterpreter)
    with SameOriginBackendClient {

  /** Call an endpoint with a payload.
    * @param endpoint
    * @param payload
    * @return
    */

  private[ziotapir] def requestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): ZIO[Any, Throwable, O] =
    requestZIO(config.baseUrl, endpoint)(payload)

    /** Call a secured endpoint with a payload.
      * @param endpoint
      * @param payload
      * @return
      */
  private[ziotapir] def securedRequestZIO[
      UserToken <: WithToken,
      I,
      E <: Throwable,
      O
  ](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O] =
    securedRequestZIO(config.baseUrl, endpoint)(payload)

  private[ziotapir] def streamRequestZIO[I, O](
      endpoint: Endpoint[Unit, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I): Task[Stream[Throwable, O]] =
    streamRequestZIO(config.baseUrl, endpoint)(payload)

  private[ziotapir] def securedStreamRequestZIO[UserToken <: WithToken, I, O](
      endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I)(using session: Session[UserToken]): Task[Stream[Throwable, O]] =
    securedStreamRequestZIO(config.baseUrl, endpoint)(payload)

}

/** The live implementation of the BackendClient.
  */
object SameOriginBackendClientLive {

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

    /** The layer that can be used to create a client.
      */
  private def layer =
    ZLayer.derive[SameOriginBackendClientLive]

  /** The layer that can be used to create
    */
  private[ziotapir] def configuredLayer
      : ZLayer[Any, Nothing, SameOriginBackendClientLive] = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    val streamInterpreter = StreamSttpClientInterpreter()
    val config = BackendClientConfig(backendBaseURL)

    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(
      streamInterpreter
    ) ++ ZLayer.succeed(
      config
    ) >>> layer
  }

}
