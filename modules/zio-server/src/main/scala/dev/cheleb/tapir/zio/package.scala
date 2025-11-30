package dev.cheleb.tapir.zio

import zio.*

import dev.cheleb.tapir.*
import sttp.capabilities.zio.ZioStreams

extension (routes: ZIO[Any, Nothing, List[BatchController[Task]]]) {
  def batchRoutes: UIO[List[sttp.tapir.server.ServerEndpoint[Any, Task]]] =
    routes.map {
      _.flatMap(_.batchRoutes)
    }
}

extension (
    routes: ZIO[Any, Nothing, List[StreamController[ZioStreams, Task]]]
) {
  def streamRoutes
      : UIO[List[sttp.tapir.server.ServerEndpoint[ZioStreams, Task]]] =
    routes.map {
      _.flatMap(_.streamRoutes)
    }
}
