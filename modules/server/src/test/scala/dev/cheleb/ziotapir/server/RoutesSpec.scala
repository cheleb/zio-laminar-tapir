package dev.cheleb.ziotapir.server

import zio.*
import zio.test.*

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.CodecFormat
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*

object RoutesSpec extends ZIOSpecDefault {

  private def simple(name: String): ServerEndpoint[Any, Task] =
    endpoint.get.in(name).out(stringBody).zServerLogic(_ => ZIO.succeed(name))

  private def streaming(name: String): ServerEndpoint[ZioStreams, Task] =
    endpoint.get
      .in(name)
      .out(streamTextBody(ZioStreams)(CodecFormat.TextPlain()))
      .zServerLogic(_ => ZIO.succeed(zio.stream.ZStream.empty))

  private class Controller(prefix: String) extends BaseController[ZioStreams] {
    override def routes = List(simple(s"$prefix-a"), simple(s"$prefix-b"))
    override def streamRoutes = List(streaming(s"$prefix-stream"))
  }

  private object EmptyController extends BaseController[ZioStreams]

  final case class Prefixes(values: List[String])

  private object TestRoutes extends Routes[Prefixes] {
    type STREAMS = ZioStreams
    protected def makeControllers =
      ZIO.serviceWith[Prefixes](
        _.values.map(Controller(_)) :+ EmptyController
      )
  }

  private def paths(endpoints: List[ServerEndpoint[?, Task]]) =
    endpoints.map(_.showShort).toSet

  def spec = suite("Routes")(
    test("a BaseController has no routes by default") {
      assertTrue(
        EmptyController.routes.isEmpty,
        EmptyController.streamRoutes.isEmpty
      )
    },
    test("endpoints gathers routes and stream routes of every controller") {
      for endpoints <- TestRoutes.endpoints
      yield assertTrue(
        endpoints.size == 6,
        paths(endpoints) == Set(
          "GET /x-a",
          "GET /x-b",
          "GET /x-stream",
          "GET /y-a",
          "GET /y-b",
          "GET /y-stream"
        )
      )
    }.provide(ZLayer.succeed(Prefixes(List("x", "y")))),
    test("endpoints is empty when controllers have no routes") {
      for endpoints <- TestRoutes.endpoints
      yield assertTrue(endpoints.isEmpty)
    }.provide(ZLayer.succeed(Prefixes(Nil)))
  )
}
