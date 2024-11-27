package dev.cheleb.ziotapir

import zio.Task

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*

trait BaseController {

  val routes: (
      List[ServerEndpoint[Any, Task]],
      List[ZServerEndpoint[Any, ZioStreams]]
  )

}
