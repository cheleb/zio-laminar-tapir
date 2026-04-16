package demo

import dev.cheleb.ziotapir.Routes
import dev.cheleb.ziotapir.BaseController
import zio.ZIO
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

object HttpApi extends Routes {

  type STREAMS = ZioStreams & WebSockets

  override protected def makeControllers
      : ZIO[Any, Nothing, List[BaseController[STREAMS]]] =
    ZIO.succeed(List(new WebSocketController()))

}
