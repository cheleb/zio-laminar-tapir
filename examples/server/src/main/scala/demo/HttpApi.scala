package demo

import dev.cheleb.ziotapir.Routes
import dev.cheleb.ziotapir.BaseController
import zio.ZIO
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import demo.hello.HelloController
import demo.hello.HelloService

object HttpApi extends Routes {

  type STREAMS = ZioStreams & WebSockets

  override type Deps = HelloService

  override protected def makeControllers
      : ZIO[HelloService, Nothing, List[BaseController[STREAMS]]] =
    for
      webSocketController <- WebSocketController.makeZIO
      helloController <- HelloController.makeZIO
    yield List(webSocketController, helloController)

}
