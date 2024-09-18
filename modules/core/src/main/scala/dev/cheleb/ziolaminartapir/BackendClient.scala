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

  /** Turn an endpoint into a function from Input => Request.
    *
    * @param endpoint
    * @return
    */

  private[ziolaminartapir] def endpointRequest[I, E, O](
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
  private[ziolaminartapir] def securedEndpointRequest[A, I, E, O](
      baseUri: Option[Uri],
      endpoint: Endpoint[A, I, E, O, Any]
  ): A => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(
      endpoint,
      baseUri
    )

  // private def isSameIssuer(token: WithToken): Option[Boolean] =
  //   for {
  //     configHost <- config.baseUrl.host
  //     confitPort <- config.baseUrl.port
  //   } yield token.issuer == s"${configHost}:${confitPort}"

  def isSameIssuer(token: WithToken): Option[Boolean] = Some(true) // FIXME

  /** Get the token from the session, or fail with an exception. */
  private[ziolaminartapir] def tokenOfFail[UserToken <: WithToken](
      issuer: Option[String]
  )(using
      session: Session[UserToken]
  ) =
    for {
      withToken <- ZIO
        .fromOption(session.getUserState(issuer))
        .orElseFail(RestrictedEndpointException("No token found"))
      sameIssuer <- ZIO
        .fromOption(isSameIssuer(withToken))
        .orElseFail(RestrictedEndpointException("No issuer found"))
      _ <- ZIO.unless(sameIssuer)(
        ZIO.fail(
          RestrictedEndpointException(
            s"Token issued by ${withToken.issuer} but backend is {config.baseUrl}"
          )
        )
      )
    } yield withToken.token

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

  def securedEndpointRequestZIO[UserToken <: WithToken, I, E <: Throwable, O](
      baseUri: Option[Uri],
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I)(using session: Session[UserToken]): ZIO[Any, Throwable, O] =
    for {
      token <- tokenOfFail(None)
      res <- backend
        .send(securedEndpointRequest(baseUri, endpoint)(token)(payload))
        .map(_.body)
        .absolve
    } yield res

}
