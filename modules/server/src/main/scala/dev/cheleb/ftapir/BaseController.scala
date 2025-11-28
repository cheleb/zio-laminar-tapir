package dev.cheleb.ftapir

import sttp.tapir.server.ServerEndpoint

trait BaseController[F[_], S] {

  /** List of routes that will be added to the server
    */
  def routes: List[ServerEndpoint[Any, F]] = Nil

  /** List of stream routes that will be added to the server
    */
  def streamRoutes: List[ServerEndpoint[S, F]] = Nil

}
