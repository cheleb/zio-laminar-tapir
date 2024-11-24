package dev.cheleb.ziotapir

import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.zio.ZioStreams

import zio.Task

trait BaseController {

  val routes: (
      List[ServerEndpoint[Any, Task]],
      List[ZServerEndpoint[Any, ZioStreams]]
  )

}
