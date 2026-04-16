package demo.hello

import zio.*

trait HelloService {
  def sayHello(): Task[String]
}

private class HelloServiceImpl extends HelloService {
  override def sayHello(): Task[String] = ZIO.succeed("Hello, World service!")
}

object HelloService {
  val live: ZLayer[Any, Nothing, HelloService] =
    ZLayer.succeed(new HelloServiceImpl())
}
