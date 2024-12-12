package dev.cheleb.ziotapir

import zio.Task

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint

trait BaseController {

  /** List of routes that will be added to the server
    */
  def routes: List[ServerEndpoint[Any, Task]] = Nil

  /** List of stream routes that will be added to the server
    */
  def streamRoutes: List[ServerEndpoint[ZioStreams, Task]] = Nil

}
