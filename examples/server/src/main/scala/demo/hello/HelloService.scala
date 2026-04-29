package demo.hello

import zio.*
import demo.GetResponse
import demo.HttpBinEndpoints
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.client.*
import zio.telemetry.opentelemetry.tracing.Tracing

trait HelloService:
  def sayHello(): Task[String]

  def askHttpBin(): Task[GetResponse]

  def boom(n: Int): Task[Unit]

private case class HelloServiceImpl()(using
    tracing: Tracing,
    backend: BackendClient
) extends HelloService {

  override def sayHello(): Task[String] = ZIO.succeed("Hello, World service!")
  override def askHttpBin(): Task[GetResponse] =
    HttpBinEndpoints
      .get(())
      .send

  override def boom(n: Int): Task[Unit] =
    tracing.span(s"deepBoom-$n)") {
      if n <= 0 then ZIO.fail(new RuntimeException("Boom!"))
      else boom(n - 1)
    }

}

object HelloService {
  val live: ZLayer[BackendClient & Tracing, Nothing, HelloService] =
    ZLayer.derive[HelloServiceImpl]
}
