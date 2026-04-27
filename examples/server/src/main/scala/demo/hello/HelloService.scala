package demo.hello

import zio.*
import demo.GetResponse
import demo.HttpBinEndpoints
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.client.*

trait HelloService {
  def sayHello(): Task[String]

  def askHttpBin(): Task[GetResponse]
}

private case class HelloServiceImpl()(using backend: BackendClient)
    extends HelloService {
  override def sayHello(): Task[String] = ZIO.succeed("Hello, World service!")
  override def askHttpBin(): Task[GetResponse] =
    HttpBinEndpoints
      .get(())
      .send
}

object HelloService {
  val live: ZLayer[BackendClient, Nothing, HelloService] =
    ZLayer.derive[HelloServiceImpl]
}
