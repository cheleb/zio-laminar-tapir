package demo

import dev.cheleb.ziotapir.server.Routes

import zio.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import demo.hello.HelloController
import demo.hello.HelloService
import dev.cheleb.ziotapir.server.BaseController
import sttp.tapir.server.ServerEndpoint
import dev.cheleb.ziotapir.client.ZIOSttpBackendLive
import sttp.model.Uri

/** This API will need the `HelloService` to create the controllers, hence the
  * context of the routes is `HelloService`. This means that when we call the
  * `endpoints` method, we will need to provide a `HelloService` in the
  * environment. See [[HttpApi.endpointsWithDeps]]
  */
object HttpApi extends Routes[HelloService] {

  type STREAMS = ZioStreams & WebSockets

  override protected def makeControllers: ZIO[HelloService, Nothing, List[
    BaseController[ZioStreams & WebSockets]
  ]] =
    for
      webSocketController <- WebSocketController.makeZIO
      helloController <- HelloController.makeZIO
    yield List(webSocketController, helloController)

  /** This method will provide the `HelloService` to the `endpoints` method, so
    * that the controllers can be created.
    */
  def endpointsWithDeps
      : Task[List[ServerEndpoint[ZioStreams & WebSockets, Task]]] =
    endpoints
      .provide(
        HelloService.live,
        ZIOSttpBackendLive.configuredLayerOn(
          Uri.unsafeParse("https://httpbin.org")
        )
      )

}

/** This API does not require any dependencies, so the context is `Any`.
  */
object HttpApiAny extends Routes[Any] {

  type STREAMS = ZioStreams & WebSockets

  override protected def makeControllers =
    for webSocketController <- WebSocketController.makeZIO
    yield List(webSocketController)

}
