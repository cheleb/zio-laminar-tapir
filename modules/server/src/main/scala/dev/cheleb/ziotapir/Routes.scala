package dev.cheleb.ziotapir

import zio.*

import sttp.tapir.server.ServerEndpoint

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
  protected def gatherRoutes[C](
      select: BaseController => List[ServerEndpoint[C, Task]]
  )(
      controllers: List[BaseController]
  ): List[ServerEndpoint[C, Task]] =
    controllers flatMap select

}
