package demo

import zio.*

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import io.github.nguyenyou.webawesome.laminar.*

import sttp.model.Uri

import zio.stream.ZStream
import sttp.ws.WebSocketFrame
import sttp.ws.WebSocketFrame.Text

val echoWebsocket: Var[Uri] = Var(Uri.unsafeParse("https://echo.websocket.org"))

def websocket =
  val hubVar: Var[Option[Hub[WebSocketFrame]]] = Var(None)
  val isNotConnected = hubVar.signal.map(_.isEmpty)

  div(
    cls := "spaced",
    children <-- hubVar.signal.map:
      case None =>
        List(
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
            )
          ),
          Button(_.variant.brand)(
            Icon(
              _.autoWidth := true,
              _.name := "plug"
            )(),
            "Connect",
            disabled <-- isNotConnected.map(!_),
            onClick --> { _ =>
              val program = for {
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

                _ = result.emit("WebSocket closed.")

              } yield ()

              program
                .run(echoWebsocket.now())

            }
          )
        )
      case Some(hub) =>
        val message = Var("")

        List(
          span(
            styleAttr := "display: flex; align-items: center; gap: 0.5rem;",
            Input(
              _.value <-- message.signal,
              _.placeholder := "Type a message to send",
              _.onInput.mapToValue --> message,
              _.disabled <-- isNotConnected
            )(
            ),
            Button(_.variant.brand)(
              Icon(
                _.autoWidth := true,
                _.name := "envelope"
              )(),
              disabled <-- isNotConnected,
              onClick --> { _ =>
                hub.sendText(message.now()).run
              }
            )
          ),
          Button(_.variant.brand)(
            Icon(
              _.autoWidth := true,
              _.name := "close"
            )(),
            "Close socket",
            disabled <-- isNotConnected,
            onClick --> { _ =>
              val close = for
                _ <- hub.closeGracefully
                _ = hubVar.set(None)
              yield ()

              close.run
            }
          )
        )
  )
