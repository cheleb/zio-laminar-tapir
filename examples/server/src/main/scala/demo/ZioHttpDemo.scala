package demo

import zio.*
import zio.http.*

import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions

object Main extends ZIOAppDefault {

  // Create the WebSocket controller
  val webSocketController = new WebSocketController()

  // Get WebSocket routes directly from the controller
  val allWebSocketRoutes = webSocketController.webSocketRoutes

  // Create the HTTP app with WebSocket support
  val httpApp =
    ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(allWebSocketRoutes)

  // The main program - start the server on port 8080
  val program = build
  // Server.serve(httpApp).provide(Server.default)

  // Run the program
  override def run: ZIO[Any & ZIOAppArgs, Any, Any] = program

  private def build: ZIO[Any, Throwable, Unit] =
    for {

      _ <- ZIO.logInfo(
        "Starting server... http://localhost:${serverConfig.port}"
      )
      apiEndpoints <- HttpApi.endpoints

      serverLayer = zio.http.Server.default
      _ <- zio.http.Server
        .serve(
          Routes(
            Method.GET / Root -> handler(
              Response.redirect(url"public/index.html")
            )
          ) ++
            ZioHttpInterpreter()
              .toHttp(
                apiEndpoints
              ) ++ httpApp
        )
        .provideSomeLayer(serverLayer) <* Console.printLine("Server started !")
    } yield ()
}
