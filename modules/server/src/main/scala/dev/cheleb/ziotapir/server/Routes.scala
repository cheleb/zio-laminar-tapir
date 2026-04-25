package dev.cheleb.ziotapir.server

import sttp.tapir.server.ServerEndpoint
import zio.*
import sttp.capabilities.Streams
import dev.cheleb.ziotapir.server.BaseController

/** A trait that provides a method to gather all the routes from a controllers.
  *
  * The controllers are created in the `makeControllers` method, which is a ZIO
  * effect that requires a context of type `Deps`. This allows the routes to
  * have access to any dependencies they need, such as a database connection or
  * a configuration object.
  *
  * @tparam Deps
  *   The type of the context that the routes require.
  */
trait Routes[Deps] {

  type STREAMS <: Streams[?]

  protected def makeControllers
      : ZIO[Deps, Nothing, List[BaseController[STREAMS]]]

  /** Gathers all the routes from a list of controllers.
    *
    * @param select
    *   A function that selects the routes from a controller.
    * @param controllers
    *   A list of controllers.
    * @tparam C
    *   The type of the context that the routes require.
    * @return
    *   A list of server endpoints.
    */
  private def gatherRoutes[C](
      controllers: List[BaseController[STREAMS]],
      select: BaseController[STREAMS] => List[ServerEndpoint[C, Task]]
  ): List[ServerEndpoint[C, Task]] =
    controllers flatMap select

  private def endpointsZIO(ctrs: List[BaseController[STREAMS]]) =
    gatherRoutes(ctrs, _.routes)

  private def streamEndpointsZIO(ctrs: List[BaseController[STREAMS]]) =
    gatherRoutes(ctrs, _.streamRoutes)

  /** This is critical, to not provide the Postgres layer too early, it would be
    * closed too early in the app lifecycle.
    */
  def endpoints: RIO[Deps, List[ServerEndpoint[STREAMS, Task]]] =
    for
      mem <- makeControllers
      endpoints = endpointsZIO(mem)
      streamEndpoints = streamEndpointsZIO(mem)
    yield endpoints ++ streamEndpoints

}
