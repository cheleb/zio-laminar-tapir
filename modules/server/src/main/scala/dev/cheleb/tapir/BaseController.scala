package dev.cheleb.tapir

import sttp.tapir.server.ServerEndpoint

trait BatchController[F[_]] {

  /** List of routes that will be added to the server
    */
  def batchRoutes: List[ServerEndpoint[Any, F]] = Nil

}

trait StreamController[C, F[_]] {

  /** List of stream routes that will be added to the server
    */
  def streamRoutes: List[ServerEndpoint[C, F]] = Nil

}
