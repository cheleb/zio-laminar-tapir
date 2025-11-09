package demo

import zio.*

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.*
import io.github.nguyenyou.webawesome.laminar.*

import sttp.model.Uri

import zio.stream.ZStream
import sttp.ws.WebSocketFrame

//val echoWebsocket: Uri = Uri.unsafeParse("https://echo.websocket.org")
val echoWebsocket: Uri = Uri.unsafeParse("http://localhost:8080")

val debugWS = Var(false)

val hubVar: Var[Option[Hub[WebSocketFrame]]] = Var(None)

def websocket =
  val message = Var("")
  div(
    Button(_.variant.brand)(
      Icon(
        _.fixedWidth := "true",
        _.name := "plug"
      )(),
      "Connect",
      disabled <-- hubVar.signal.map(_.isDefined),
      onClick --> { _ =>
        val program = for {
          _ <- result.zEmit("Connecting to WebSocket...")

          ws <- WebsocketEndpoint.echo
            .responseZIO(())
            .asWebSocketStream(debug = debugWS.now())

          hub <- Hub.unbounded[WebSocketFrame]

          _ = hubVar.set(Some(hub))

          _ <- ws(
            ZStream
              .fromHubWithShutdown(hub)
              .interruptWhen(hub.awaitShutdown)
              .tap(msg => result.zEmit(s"Sending: $msg"))
          )
            .runForeach(msg => result.zEmit(s"Received: $msg"))

          _ = result.emit("WebSocket closed.")

        } yield ()

        program.run(echoWebsocket)

      }
    ),
    span(
      styleAttr := "display: flex; align-items: center; gap: 0.5rem;",
      Input(
        _.value <-- message.signal,
        _.placeholder := "Type a message to send",
        _.onInput.mapToValue --> message,
        _.disabled <-- hubVar.signal.map(_.isEmpty)
      )(
      ),
      Button(_.variant.brand)(
        Icon(
          _.fixedWidth := "true",
          _.name := "envelope"
        )(),
        disabled <-- hubVar.signal.map(_.isEmpty),
        onClick --> { _ =>
          hubVar
            .now()
            .foreach:
              _.offer(WebSocketFrame.text(message.now())).run
        }
      )
    ),
    Button(_.variant.brand)(
      Icon(
        _.fixedWidth := "true",
        _.name := "close"
      )(),
      "Close socket",
      disabled <-- hubVar.signal.map(_.isEmpty),
      onClick --> { _ =>
        hubVar.now() match {
          case Some(hub) =>
            val close = for
              _ <- hub.offer(WebSocketFrame.close)
              _ <- hub.shutdown
              _ = hubVar.set(None)
            yield ()

            close.run
          case None =>
            ZIO.unit
        }

      }
    ),
    input(
      typ := "checkbox",
      onChange.mapToChecked --> { debug =>
        result.emit(s"WebSocket debug mode: $debug")
        debugWS.set(debug)
      }
    )
  )

def websocketClient =
  val message = Var("")
  div(
    Button(_.variant.brand)(
      Icon(
        _.fixedWidth := "true",
        _.name := "plug"
      )(),
      "Connect",
      disabled <-- hubVar.signal.map(_.isDefined),
      onClick --> { _ =>
        val program = for {
          _ <- result.zEmit("Connecting to WebSocket...")

          ws <- WebsocketEndpoint.echo(())

          hub <- Hub.unbounded[WebSocketFrame]

          _ = hubVar.set(Some(hub))

          _ <- ws(
            ZStream
              .unwrapScoped(
                ZStream
                  .fromHubScoped(hub)
              )
              .tap(msg => result.zEmit(s"Sending: $msg"))
          )
            .runForeach(msg => result.zEmit(s"Received: $msg"))

          _ = result.emit("WebSocket closed.")

        } yield ()

        program
          .run(echoWebsocket)

      }
    ),
    span(
      styleAttr := "display: flex; align-items: center; gap: 0.5rem;",
      Input(
        _.value <-- message.signal,
        _.placeholder := "Type a message to send",
        _.onInput.mapToValue --> message,
        _.disabled <-- hubVar.signal.map(_.isEmpty)
      )(
      ),
      Button(_.variant.brand)(
        Icon(
          _.fixedWidth := "true",
          _.name := "envelope"
        )(),
        disabled <-- hubVar.signal.map(_.isEmpty),
        onClick --> { _ =>
          hubVar
            .now()
            .foreach:
              _.offer(WebSocketFrame.text(message.now())).run
        }
      )
    ),
    Button(_.variant.brand)(
      Icon(
        _.fixedWidth := "true",
        _.name := "close"
      )(),
      "Close socket",
      disabled <-- hubVar.signal.map(_.isEmpty),
      onClick --> { _ =>
        hubVar.now() match {
          case Some(hub) =>
            val close = for
              _ <- hub.offer(WebSocketFrame.close)
              _ <- hub.shutdown
              _ = hubVar.set(None)
            yield ()

            close.run
          case None =>
            ZIO.unit
        }

      }
    ),
    input(
      typ := "checkbox",
      onChange.mapToChecked --> { debug =>
        result.emit(s"WebSocket debug mode: $debug")
        debugWS.set(debug)
      }
    )
  )
