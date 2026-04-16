package demo

import zio.*
import zio.http.*

import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions

object Main extends ZIOAppDefault {

  // The main program - start the server on port 8080
  val program = for
    _ <- Console.printLine("Starting server on http://localhost:8080")
    endpointsNoDeps <- HttpApiAny.endpoints
    endpoints <- HttpApi.endpoints
    httpApp = ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(
      endpoints ++ endpointsNoDeps
    )
    _ <- Server.serve(httpApp).provide(Server.default)
  yield ()

  // Run the program
  override def run: ZIO[Any & ZIOAppArgs, Any, Any] = program
  // .provide(HelloService.live)

}
