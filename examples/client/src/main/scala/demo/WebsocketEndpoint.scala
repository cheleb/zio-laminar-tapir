package demo

//import zio.json.*

import sttp.tapir.*
//import sttp.tapir.generic.auto.*
//import sttp.tapir.json.zio.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

object WebsocketEndpoint extends BaseEndpoint {
  val wsEndpoint: PublicEndpoint[
    Unit,
    Unit,
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
}
