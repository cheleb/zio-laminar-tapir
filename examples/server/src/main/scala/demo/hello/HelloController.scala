package demo.hello

import zio.*

import sttp.tapir.*

import dev.cheleb.ziotapir.BaseController
import sttp.capabilities.zio.ZioStreams

case class HelloController(dep: HelloService)
    extends BaseController[ZioStreams]
    with HelloEndpoints {

  val hello = endpoint.get
    .in("hello")
    .out(stringBody)
    .description("A simple hello endpoint")
}

object HelloController:
  def makeZIO =
    ZIO.service[HelloService].map(HelloController(_))
