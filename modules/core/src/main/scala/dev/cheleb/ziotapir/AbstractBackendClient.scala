package dev.cheleb.ziotapir

import zio.*
import zio.stream.*

import dev.cheleb.ziojwt.WithToken
import sttp.capabilities.zio.ZioStreams
import sttp.client4.*
import sttp.model.Uri
import sttp.tapir.Endpoint

import laminar.Session
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter

/** A client to the backend, extending the endpoints as methods.
  */

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private[ziotapir] abstract class AbstractBackendClient(
    backend: WebSocketStreamBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    streamInterpreter: StreamSttpClientInterpreter
) {

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

}
