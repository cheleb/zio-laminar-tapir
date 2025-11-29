package dev.cheleb.ftapir

//import sttp.capabilities.Streams
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
trait Routes[C, F[_]] {

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
  protected def gatherBatchRoutes(
      select: BaseController[Any, F] => List[ServerEndpoint[Any, F]]
  )(
      controllers: List[BaseController[Any, F]]
  ): List[ServerEndpoint[Any, F]] =
    controllers flatMap select

  protected def gatherStreamRoutes(
      select: BaseController[C, F] => List[ServerEndpoint[C, F]]
  )(
      controllers: List[BaseController[C, F]]
  ): List[ServerEndpoint[C, F]] =
    controllers flatMap select

}
