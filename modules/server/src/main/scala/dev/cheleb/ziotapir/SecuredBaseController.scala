package dev.cheleb.ziotapir

import zio.*

import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*

/** A base controller for all secured endpoints
  *
  * @param principalExtractor
  *   the function to extract the principal from the Security Input
  */
trait SecuredBaseController[SI, Principal](
    principalExtractor: SI => Task[Principal]
) extends BaseController:
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
    def zServerAuthenticatedLogic(
        logic: Principal => I => Task[O]
    ): ServerEndpoint[R, Task] =
      endpoint
        .zServerSecurityLogic(principalExtractor)
        .serverLogic(principal => input => logic(principal)(input))
