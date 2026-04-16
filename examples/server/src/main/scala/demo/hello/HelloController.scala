package demo.hello

import zio.*

import dev.cheleb.ziotapir.BaseController
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ServerEndpoint

case class HelloController(dep: HelloService)
    extends BaseController[ZioStreams]
    with HelloEndpoints {

  val hello = helloEndpoint.serverLogicSuccess(_ => dep.sayHello())

  override def routes: List[ServerEndpoint[Any, Task]] =
    List(hello)
}

object HelloController:
  def makeZIO =
    ZIO.service[HelloService].map(HelloController(_))
