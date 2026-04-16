package dev.cheleb.ziotapir

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import zio.*

/** A trait that provides a method to gather all the routes from a controllers.
  *
  * The method `gatherRoutes` takes a function that selects the routes from a
  * controller, the type parameter `C` is the type of the context that the
  * routes require (Streams, WebSockets, etc.).
  *
  * @tparam C
  *   The type of the context that the routes require.
  */
trait Routes {

  type STREAMS <: ZioStreams

  type Deps <: Any

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
      select: BaseController[STREAMS] => List[ServerEndpoint[C, Task]]
  )(
      controllers: List[BaseController[STREAMS]]
  ): List[ServerEndpoint[C, Task]] =
    controllers flatMap select

  private def endpointsZIO(ctrs: List[BaseController[STREAMS]]) =
    gatherRoutes(_.routes)(ctrs)

  private def streamEndpointsZIO(ctrs: List[BaseController[STREAMS]]) =
    gatherRoutes(_.streamRoutes)(ctrs)

  protected def gatherAllRoutes
      : URIO[Deps, List[ServerEndpoint[STREAMS, Task]]] =
    for {
      mem <- makeControllers
      endpoints = endpointsZIO(mem)
      streamEndpoints = streamEndpointsZIO(mem)
    } yield endpoints ++ streamEndpoints

  /** This is critical, to not provide the Postgres layer too early, it would be
    * closed too early in the app lifecycle.
    */
  def endpoints: ZIO[Deps, Throwable, List[ServerEndpoint[STREAMS, Task]]] =
    gatherAllRoutes
}
