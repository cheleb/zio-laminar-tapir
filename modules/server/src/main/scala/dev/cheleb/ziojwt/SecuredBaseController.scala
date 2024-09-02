package dev.cheleb.ziojwt

import zio.*

import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*

/** A base controller for all secured endpoints
  *
  * @param jwtService
  */
trait SecuredBaseController[SI, Principal](principal: SI => Task[Principal]):
  /** Enriches an endpoint with security logic
    */
  extension [I, O, R](endpoint: Endpoint[SI, I, Throwable, O, R])
    /** ZIO security logic for a server endpoint
      *
      * Extracts the user ID from the request and verifies the JWT token
      * @param logic,
      *   curryied function from user ID to request to response
      * @return
      */
    def securedServerLogic(
        logic: Principal => I => Task[O]
    ): ServerEndpoint[R, Task] =
      endpoint
        .zServerSecurityLogic(token => principal(token))
        .serverLogic(userId => input => logic(userId)(input))
