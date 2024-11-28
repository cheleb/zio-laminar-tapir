package dev.cheleb.ziotapir

import zio.*
import sttp.tapir.server.ServerEndpoint

trait Routes {

  protected def gatherRoutes[C](
      select: BaseController => List[ServerEndpoint[C, Task]]
  )(
      controllers: List[BaseController]
  ): List[ServerEndpoint[C, Task]] =
    controllers flatMap select

}
