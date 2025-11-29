package demo

//import zio.json.*

import sttp.tapir.*
//import sttp.tapir.generic.auto.*
//import sttp.tapir.json.zio.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.JsonCodec
import sttp.ws.WebSocketFrame

case class Error(description: String) derives JsonCodec, Schema
object WebsocketEndpoint extends BaseEndpoint {
  val echo: PublicEndpoint[
    Unit,
    Error,
    ZioStreams.Pipe[WebSocketFrame, WebSocketFrame],
    ZioStreams & WebSockets
  ] =
    endpoint.get
      .in("ws" / "echo")
      .out(
        webSocketBody[
          WebSocketFrame,
          CodecFormat.TextPlain,
          WebSocketFrame,
          CodecFormat.TextPlain
        ](
          ZioStreams
        )
      )
      .errorOut(jsonBody[Error])
      .description("WebSocket echo endpoint")
}
