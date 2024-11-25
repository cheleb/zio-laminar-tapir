package dev.cheleb.ziotapir

import dev.cheleb.ziojwt.WithToken

import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import zio.*
import zio.stream.*
import sttp.model.Uri
import laminar.Session

/** A client to the backend, extending the endpoints as methods.
  */

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private[ziotapir] abstract class BackendClient(
    backend: SttpBackend[Task, ZioStreamsWithWebSockets],
    interpreter: SttpClientInterpreter
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
  private[ziotapir] def endpointRequest[I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, Some(baseUri))

  private[ziotapir] def streamRequest[I, O](
      baseUri: Uri,
      endpoint: Endpoint[
        Unit,
        I,
        Throwable,
        Stream[Throwable, O],
        ZioStreams
      ]
  ): I => Request[Either[Throwable, Stream[Throwable, O]], ZioStreams] =
    interpreter.toRequestThrowDecodeFailures(endpoint, Some(baseUri))

  /** Turn a secured endpoint into curried functions:
    *
    * {{{
    *  SecurityInput => Input => Request.
    * }}}
    *
    * @param endpoint
    * @return
    */
  private[ziotapir] def securedEndpointRequest[SI, I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[SI, I, E, O, Any]
  ): SI => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(
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

  private[ziotapir] def endpointRequestZIO[I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): Task[O] =
    backend
      .send(endpointRequest(baseUri, endpoint)(payload))
      .map(_.body)
      .absolve

  private[ziotapir] def securedEndpointRequestZIO[
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
          securedEndpointRequest(baseUri, endpoint)(token)(payload)
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

}
