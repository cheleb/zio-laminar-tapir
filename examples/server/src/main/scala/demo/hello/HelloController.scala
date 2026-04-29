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
import zio.telemetry.opentelemetry.tracing.Tracing
import sttp.tapir.ztapir.*

case class HelloController(dep: HelloService)(using tracing: Tracing)
    extends BaseController[ZioStreams]
    with HelloEndpoints {

  extension [A](task: Task[A])
    def toApi: IO[ErrorResponse, A] =
      task.mapError(e => ErrorResponse(e.getMessage))

  import tracing.aspects.*

  val hello: ServerEndpoint[Any, Task] =
    helloEndpoint.serverLogicSuccess: _ =>
      dep.sayHello() @@ span("hello-endpoint")

  val proxy: ServerEndpoint[Any, Task] =
    proxyEndpoint.zServerLogic(_ =>
      dep.askHttpBin().toApi
        @@ span("proxy-endpoint")
    )

  val streaming: ServerEndpoint[ZioStreams, Task] =
    streamingEndpoint.serverLogicSuccess(_ =>
      ZIO.succeed:
        "application/jsonl" ->
          ZStream
            .fromIterable(1 to 100)
            .map(i => Organisation(UUID.randomUUID(), s"Test $i", None))
            .toJsonArrayStream
    )

  val boom: ServerEndpoint[Any, Task] =
    boomEndpoint.zServerLogic(_ =>
      dep.boom().mapError(e => ErrorResponse(e.getMessage)) @@ span(
        "boom-endpoint"
      )
    )

  override def routes: List[ServerEndpoint[Any, Task]] =
    List(hello, proxy, boom)

  override def streamRoutes: List[ServerEndpoint[ZioStreams, Task]] =
    List(streaming)
}

object HelloController:
  def makeZIO(using tracing: Tracing) =
    ZIO.service[HelloService].map(HelloController(_))
