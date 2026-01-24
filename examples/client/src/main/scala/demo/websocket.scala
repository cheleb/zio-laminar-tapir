package demo

import zio.*

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import dev.cheleb.ziotapir.awesomelaminar.*
import io.github.nguyenyou.webawesome.laminar.*

import sttp.model.Uri

import zio.stream.ZStream
import sttp.ws.WebSocketFrame
import sttp.ws.WebSocketFrame.Text

val echoWebsocket: Var[Uri] = Var(
  Uri.unsafeParse("https://echo.websocket.org")
)

def websocket =
  val hubVar: Var[Option[Hub[WebSocketFrame]]] = Var(None)

  div(
    cls := "spaced",
    children <-- hubVar.signal.map:
      case None =>
        List(
          chooseServer(),
          ZButton("Connect", "plug", echoWebsocket.now()):
            for {
              _ <- result.zEmit("Connecting to WebSocket...")

              ws <- WebsocketEndpoint.echo(())

              hub <- Hub.unbounded[WebSocketFrame]

              _ = hubVar.set(Some(hub))

              _ <- ws(
                ZStream
                  .fromHubWithShutdown(hub)
                  .tap(msg => result.zEmit(s"Sending: $msg"))
              )
                .runForeach {
                  case Text(payload = payload) =>
                    result.zEmit(s"Received: $payload")
                  case _ => ZIO.unit
                }

              _ <- result.zEmit("WebSocket closed.")

            } yield ()
        )

      case Some(hub) =>
        val message = Var("")

        List(
          span(
            cls := "flex",
            Input(
              _.value <-- message.signal,
              _.placeholder := "Type a message to send",
              _.onInput.mapToValue --> message
            )(
            ),
            ZButton("Send", "envelope"):
              hub.sendText(message.now())
          ),
          ZButton("Close socket", "close"):
            for
              _ = hubVar.set(None)
              _ <- hub.closeGracefully
            yield ()
        )
  ).withSnippet:
    """|for
       |  // ws is a function OutputStream => InputStream 
       |   ws <- WebsocketEndpoint.echo(())
       |
       |  // Create a Hub to let UI send messages.
       |   hub <- Hub.unbounded[WebSocketFrame]   
       |  // The Hub is a publish-subscribe structure.
       |  // We can create a Stream from it that and
       |  // provide this Stream to the ws function.
       |  // This will allow us to open the connection
       |  // and receive an Input Stream for
       |  // incoming messages.
       |   _ <- ws(
       |      ZStream
       |       .fromHubWithShutdown(hub)
       |       .tap(msg => result.zEmit(s"Sending: $msg"))
       |      ).runForeach { // Loop to process incoming messages
       |         case Text(payload = payload) =>
       |             result.zEmit(s"Received: $payload")
       |         case _ => ZIO.unit
       |     }
       |  // WebSocket closes...Loop ends.
       |   _ <- result.zEmit("WebSocket closed.")
       |yield ()
  """

def chooseServer(): Dropdown.Element =
  Dropdown(
    _.onSelect
      .map(
        _.detail.item.value.toRight("Nope").flatMap(Uri.parse)
      ) --> Observer[Either[String, Uri]] {
      case Right(uri) =>
        echoWebsocket.set(uri)
        result.emit(s"Selected WebSocket URI: $uri")
      case Left(_) =>
        result.emit("Invalid WebSocket URI selected.")
    },
    _.slots.trigger(
      Button(
        _.withCaret := true
      )(
        child <-- echoWebsocket.signal.map(_.toString)
      )
    )
  )(
    DropdownItem(_.value := "https://echo.websocket.org")(
      "https://echo.websocket.org"
    ),
    DropdownItem(_.value := "http://localhost:8080")(
      "http://localhost:8080"
    ),
    DropdownItem(_.value := "ws://localhost:8080")(
      "ws://localhost:8080"
    )
  )
