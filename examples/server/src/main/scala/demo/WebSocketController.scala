package demo

import zio.*
import zio.stream.*

import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.server.ServerEndpoint
import sttp.ws.WebSocketFrame

import dev.cheleb.ziotapir.BaseController

class WebSocketController extends BaseController {

  // Implement the echo WebSocket endpoint
  val echoServerEndpoint: ServerEndpoint[ZioStreams & WebSockets, Task] =
    WebsocketEndpoint.echo.serverLogicSuccess { _ =>
      // Create a pipe that echoes back any WebSocket frame it receives
      ZIO.debug("New WebSocket connection established.") *>
        ZIO.succeed((input: ZStream[Any, Throwable, WebSocketFrame]) =>
          input
            .map { frame =>
              // For echo, just return the same frame
              frame
            }
            .tap(_ => ZIO.debug("Echoed a WebSocket frame."))
            ++ ZStream
              .succeed(WebSocketFrame.text("Goodbye!"))
              .tap(_ => ZIO.debug("Sent closing message."))
          // Ensure the stream ends with a Close frame
        ) // <* ZIO.debug("WebSocket connection closed.")
    }

  // WebSocket routes with WebSockets capability
  def webSocketRoutes: List[ServerEndpoint[ZioStreams & WebSockets, Task]] =
    List(echoServerEndpoint)
}
