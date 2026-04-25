package demo.hello

import zio.*
import zio.json.*
import dev.cheleb.ziotapir.server.*

import dev.cheleb.ziotapir.server.BaseController
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint
import zio.stream.ZStream
import demo.Organisation
import java.util.UUID

case class HelloController(dep: HelloService)
    extends BaseController[ZioStreams]
    with HelloEndpoints {

  val hello: ServerEndpoint[Any, Task] =
    helloEndpoint.serverLogicSuccess(_ => dep.sayHello())

  val streaming: ServerEndpoint[ZioStreams, Task] =
    streamingEndpoint.serverLogicSuccess(_ =>
      ZIO.succeed:
        "application/jsonl" ->
          ZStream
            .fromIterable(1 to 100)
            .map(i => Organisation(UUID.randomUUID(), s"Test $i", None))
            .toJsonArrayStream
    )

  override def routes: List[ServerEndpoint[Any, Task]] =
    List(hello)

  override def streamRoutes: List[ServerEndpoint[ZioStreams, Task]] =
    List(streaming)
}

object HelloController:
  def makeZIO =
    ZIO.service[HelloService].map(HelloController(_))
