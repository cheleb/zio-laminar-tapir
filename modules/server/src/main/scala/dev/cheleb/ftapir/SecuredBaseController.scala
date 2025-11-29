package dev.cheleb.ftapir

import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

/** A base controller for all secured endpoints
  *
  * @param principalExtractor
  *   the function to extract the principal from the Security Input
  */
trait SecuredBaseController[C, F[_], SI, Principal](
    principalExtractor: SI => F[Either[Throwable, Principal]]
) extends StreamController[C, F]:
  /** Enriches an endpoint with security logic
    */
  extension [I, O, R](endpoint: Endpoint[SI, I, Throwable, O, R])
    /** ZIO security logic for a server endpoint
      *
      * Extracts the user principal from the request Security Input, and applies
      *
      * @param logic,
      *   curryied function from user principal to request to response
      * @return
      */
    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def serverAuthenticatedLogic(
        logic: Principal => I => F[Either[Throwable, O]]
    ): ServerEndpoint[R, F] =
      endpoint
        .serverSecurityLogic(principalExtractor)
        .serverLogic(principal => input => logic(principal)(input))
