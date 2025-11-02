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

case class Error(description: String) derives JsonCodec, Schema
object WebsocketEndpoint extends BaseEndpoint {
  val wsEndpoint: PublicEndpoint[
    Unit,
    Error,
    ZioStreams.Pipe[String, String],
    ZioStreams & WebSockets
  ] =
    endpoint.get
      .out(
        webSocketBody[
          String,
          CodecFormat.TextPlain,
          String,
          CodecFormat.TextPlain
        ](
          ZioStreams
        )
      )
      .errorOut(jsonBody[Error])
      .description("WebSocket echo endpoint")
}
