package dev.cheleb.ziolaminartapir

import dev.cheleb.ziojwt.WithToken

import sttp.client3.*
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter

import zio.*
import sttp.model.Uri

/** A client to the backend, extending the endpoints as methods.
  */

/** The live implementation of the BackendClient.
  *
  * @param backend
  * @param interpreter
  * @param config
  */
private[ziolaminartapir] abstract class BackendClient(
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
  private[ziolaminartapir] def endpointRequest[I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] =
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
  private[ziolaminartapir] def securedEndpointRequest[SI, I, E, O](
      baseUri: Uri,
      endpoint: Endpoint[SI, I, E, O, Any]
  ): SI => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(
      endpoint,
      Some(baseUri)
    )

  /** Get the token from the session, or fail with an exception. */
  private[ziolaminartapir] def tokenOfFail[UserToken <: WithToken](
      issuer: Uri
  )(using
      session: Session[UserToken]
  ) =
    for {
      withToken <- ZIO
        .fromOption(session.getToken(issuer))
        .orElseFail(RestrictedEndpointException("No token found"))

    } yield withToken.token

  private[ziolaminartapir] def endpointRequestZIO[I, E <: Throwable, O](
      baseUri: Uri,
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(
      payload: I
  ): ZIO[Any, Throwable, O] =
    backend
      .send(endpointRequest(baseUri, endpoint)(payload))
      .map(_.body)
      .absolve

  private[ziolaminartapir] def securedEndpointRequestZIO[
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

}
