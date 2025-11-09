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
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp4.ws.WebSocketSttpClientInterpreter

import sttp.capabilities.Streams
import sttp.tapir.client.sttp4.WebSocketToPipe

/** A client to the backend, extending the endpoints as methods.
  */
trait BackendClient {

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

  private[ziotapir] def wsResponseZIO[I, E, WI, WO, R <: Streams[
    ?
  ] & WebSockets](
      wse: Endpoint[
        Unit,
        I,
        E,
        ZioStreams.Pipe[WI, WO],
        R
      ]
  )(payload: I)(using
      WebSocketToPipe[R]
  ): Task[Response[
    ZStream[Any, Throwable, WI] => ZStream[Any, Throwable, WO]
  ]]

  private[ziotapir] def wsClientZIO[I, E, WI, WO, R <: Streams[
    ?
  ] & WebSockets](
      wse: Endpoint[
        Unit,
        I,
        E,
        ZioStreams.Pipe[WI, WO],
        R
      ]
  )(payload: I)(using
      WebSocketToPipe[R]
  ): Task[ZioStreams.Pipe[WI, WO]]
}

/** A client to the backend, extending the endpoints as methods.
  */

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private class BackendClientLive(
    backend: WebSocketStreamBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    streamInterpreter: StreamSttpClientInterpreter,
    websocketInterpreter: WebSocketSttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {

  /** Turn an endpoint into a function:
    *
    * {{{
    * Input => Request
    * }}}
    *
    * @param endpoint
    * @return
    */
  private[ziotapir] def request[I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O]] =
    interpreter.toRequestThrowDecodeFailures(endpoint, Some(baseUri))

  /** Turn a stream endpoint into a function:
    * {{{
    * Input => Request
    * }}}
    *
    * @param baseUri
    * @param endpoint
    * @return
    */
  private[ziotapir] def streamRequest[I, O](
      baseUri: Uri,
      endpoint: Endpoint[
        Unit,
        I,
        Throwable,
        Stream[Throwable, O],
        ZioStreams
      ]
  ): I => StreamRequest[Either[Throwable, Stream[Throwable, O]], ZioStreams] =
    streamInterpreter.toRequestThrowDecodeFailures(endpoint, Some(baseUri))

  /** Turn a secured endpoint into curried functions:
    *
    * {{{
    *  SecurityInput => Input => Request.
    * }}}
    *
    * @param endpoint
    * @return
    */
  private[ziotapir] def securedRequest[SI, I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[SI, I, E, O, Any]
  ): SI => I => Request[Either[E, O]] =
    interpreter.toSecureRequestThrowDecodeFailures(
      endpoint,
      Some(baseUri)
    )

  /** Turn a secured stream endpoint into curried functions:
    *
    * {{{
    *  SecurityInput => Input => Request.
    * }}}
    *
    * @param baseUri
    * @param endpoint
    * @return
    */
  private[ziotapir] def securedStreamRequest[SI, I, O](
      baseUri: Uri,
      endpoint: Endpoint[SI, I, Throwable, Stream[Throwable, O], ZioStreams]
  ): SI => I => StreamRequest[
    Either[Throwable, Stream[Throwable, O]],
    ZioStreams
  ] =
    streamInterpreter.toSecureRequestThrowDecodeFailures(
      endpoint,
      Some(baseUri)
    )

  /** Get the token from the session, or fail with an exception. */
  private[ziotapir] def tokenOfFail[UserToken <: WithToken](
      issuer: Uri
  )(using
      session: Session[UserToken]
  ) =
    for {
      withToken <- ZIO
        .fromOption(session.getToken(issuer))
        .orElseFail(RestrictedEndpointException("No token found"))

    } yield withToken.token

  /** Call an endpoint with a payload, and get a ZIO back.
    *
    * @param baseUri
    * @param endpoint
    * @param payload
    * @return
    */
  private[ziotapir] def requestZIO[I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O] =
    backend
      .send(request(baseUri, endpoint)(payload))
      .map(_.body)
      .absolve

  /** Call a secured endpoint with a payload, and get a ZIO back.
    *
    * @param baseUri
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
      baseUri: Uri,
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O] =
    for {
      token <- tokenOfFail(baseUri)
      res <- backend
        .send(
          securedRequest(baseUri, endpoint)(token)(payload)
        )
        .map(_.body)
        .absolve
    } yield res

  private[ziotapir] def streamRequestZIO[I, O](
      baseUri: Uri,
      endpoint: Endpoint[
        Unit,
        I,
        Throwable,
        Stream[Throwable, O],
        ZioStreams
      ]
  )(payload: I): Task[Stream[Throwable, O]] =
    backend
      .send(streamRequest(baseUri, endpoint)(payload))
      .map(_.body)
      .absolve

  def securedStreamRequestZIO[UserToken <: WithToken, I, O](
      baseUri: Uri,
      endpoint: Endpoint[String, I, Throwable, Stream[Throwable, O], ZioStreams]
  )(payload: I)(using session: Session[UserToken]): Task[Stream[Throwable, O]] =
    for {
      token <- tokenOfFail(baseUri)
      res <- backend
        .send(
          securedStreamRequest(baseUri, endpoint)(token)(payload)
        )
        .map(_.body)
        .absolve
    } yield res

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

  def websocketClient[I, E, WI, WO, R <: Streams[?] & WebSockets](
      wse: Endpoint[Unit, I, E, ZioStreams.Pipe[WI, WO], R]
  )(using
      WebSocketToPipe[R]
  ): I => Task[
    ZioStreams.Pipe[WI, WO]
  ] =
    websocketInterpreter.toClientThrowErrors(wse, Some(config.baseUrl), backend)

  /** Turn a websocket endpoint into a function that builds a WebSocketRequest
    * from an arbitrary input.
    * @param wse
    * @return
    */
  private def websocketRequest[I, E, WI, WO, R <: Streams[?] & WebSockets](
      wse: Endpoint[Unit, I, E, ZioStreams.Pipe[WI, WO], R]
  )(using
      WebSocketToPipe[R]
  ): I => WebSocketRequest[
    Task,
    ZioStreams.Pipe[WI, WO]
  ] =
    websocketInterpreter.toRequestThrowErrors(wse, Some(config.baseUrl))

  /** Call a websocket endpoint with a payload, and get a ZIO back.
    *
    * The returned ZIO, when executed, will open the websocket connection and
    * return a Response containing a function to process the websocket stream.
    *
    * @param wse
    * @param payload
    * @return
    */
  private[ziotapir] def wsResponseZIO[I, E, WI, WO, R <: Streams[
    ?
  ] & WebSockets](
      wse: Endpoint[
        Unit,
        I,
        E,
        ZioStreams.Pipe[WI, WO],
        R
      ]
  )(payload: I)(using
      WebSocketToPipe[R]
  ): ZIO[Any, Throwable, Response[
    ZioStreams.Pipe[WI, WO]
  ]] =

    websocketRequest(wse)(payload)
      .send(backend)

  private[ziotapir] def wsClientZIO[I, E, WI, WO, R <: Streams[?] & WebSockets](
      wse: Endpoint[
        Unit,
        I,
        E,
        ZioStreams.Pipe[WI, WO],
        R
      ]
  )(payload: I)(using
      WebSocketToPipe[R]
  ): Task[ZioStreams.Pipe[WI, WO]] =

    websocketClient(wse)(payload)

}

/** The live implementation of the BackendClient.
  */
object BackendClientLive {

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

  /** Convenience method to create a URL for the backend.
    *
    * The base URL of the backend.
    */
  def url(paths: String*): Uri =
    backendBaseURL.addPath(paths)

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
