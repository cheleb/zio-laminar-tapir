package dev.cheleb.ziotapir

import sttp.tapir.server.ServerEndpoint
import zio.Task

trait BaseController {

  val routes: List[ServerEndpoint[Any, Task]]

}
