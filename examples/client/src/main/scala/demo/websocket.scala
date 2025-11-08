package demo

import zio.*

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.*

import sttp.model.Uri

import zio.stream.ZStream
import sttp.ws.WebSocketFrame

val echoWebsocket: Uri = Uri.unsafeParse("https://echo.websocket.org")
//val echoWebsocket: Uri = Uri.unsafeParse("http://localhost:8080")
val newMesageBus = new EventBus[String]()
val queue = Queue.unbounded[WebSocketFrame].runSyncUnsafe()
val debugWS = Var(false)
val closeWSVar = Var(Option.empty[Promise[Nothing, Unit]])

def websocket = div(
  span(
    hr(),
    button(
      "WebSocket",
      disabled <-- closeWSVar.signal.map(_.isDefined),
      onClick --> { _ =>
        val closeWS = Promise.make[Nothing, Unit].runSyncUnsafe()
        closeWSVar.set(Some(closeWS))

        val program = for {
          _ <- result.zEmit("Connecting to WebSocket...")

          ws <- WebsocketEndpoint
            .echo(())
            .asWebSocketStream(debug = debugWS.now())

          _ <- ws(
            ZStream
              .fromQueue(queue)
              .interruptWhen(closeWS)
              .tap(msg => result.zEmit(s"Sending: $msg"))
          )
            .runForeach(msg => result.zEmit(s"Received: $msg"))

          _ = result.emit("WebSocket closed.")

        } yield ()

        program.run(echoWebsocket)

      }
    )
  ),
  input(
    typ := "checkbox",
    onChange.mapToChecked --> { debug =>
      result.emit(s"WebSocket debug mode: $debug")
      debugWS.set(debug)
    }
  ),
  button(
    "Send message",
    disabled <-- closeWSVar.signal.map(_.isEmpty),
    onClick --> { _ =>
      queue.offer(WebSocketFrame.text("Hello from client!")).run
    }
  ),
  button(
    "Close WebSocket",
    disabled <-- closeWSVar.signal.map(_.isEmpty),
    onClick --> { _ =>
      queue.offer(WebSocketFrame.close).run
      closeWSVar.now().foreach(_.succeed(()).run)
      closeWSVar.set(None)
    }
  ),
  div(
    h3("WebSocket Messages:"),
    child <-- newMesageBus.events.map(msg => p(s"Sent: $msg"))
  )
)
