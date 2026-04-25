package dev.cheleb.ziotapir.server

import zio.Task

import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.Streams

/** Base trait for all controllers.
  *
  * This trait provides a list of routes that will be added to the server. It
  * allows simple aggregation in the server see
  * [[dev.cheleb.ziotapir.server.Routes#gatherRoutes]] for more information.
  */
trait BaseController[-STREAMS <: Streams[?]] {

  /** List of routes that will be added to the server
    */
  def routes: List[ServerEndpoint[Any, Task]] = Nil

  /** List of stream routes that will be added to the server
    */
  def streamRoutes: List[ServerEndpoint[STREAMS, Task]] = Nil

}
