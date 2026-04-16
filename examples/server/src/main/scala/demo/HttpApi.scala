package demo

import dev.cheleb.ziotapir.Routes
import dev.cheleb.ziotapir.BaseController
import zio.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import demo.hello.HelloController
import demo.hello.HelloService
import sttp.tapir.server.ServerEndpoint

object HttpApi extends Routes[HelloService] {

  type STREAMS = ZioStreams & WebSockets

  override protected def makeControllers
      : ZIO[HelloService, Nothing, List[BaseController[STREAMS]]] =
    for
      webSocketController <- WebSocketController.makeZIO
      helloController <- HelloController.makeZIO
    yield List(webSocketController, helloController)
//    yield List(webSocketController)

  override def endpoints: Task[List[ServerEndpoint[STREAMS, Task]]] =
    endpointsWithDeps.provide(HelloService.live)
}
