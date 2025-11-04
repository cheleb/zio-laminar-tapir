package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.*
import org.scalajs.dom
import sttp.model.Uri

import zio.stream.ZStream
import sttp.ws.WebSocketFrame

given httpbin: Uri = Uri.unsafeParse("https://httpbin.org")
given echoWebsocket: Uri = Uri.unsafeParse("https://echo.websocket.org")
val localhost = Uri.unsafeParse(dom.window.location.origin)

var result = EventBus[String]()

val myApp =
  val eventBus = new EventBus[GetResponse]()
  val newMesageBus = new EventBus[String]()
  val queue = Queue.unbounded[WebSocketFrame].runSyncUnsafe()
  val debugWS = Var(false)
  val closeWS = Promise.make[Nothing, Unit].runSyncUnsafe()

  div(
    div(
      h1("ZIO and Tapir."),
      div(
        button(
          s"Streaming jsonl sameorigin ($localhost)",
          onClick --> (_ =>
            LocalEndpoints
              .allStream(())
              .jsonlEither[Organisation]: organisation =>
                result.emit(organisation.toJsonPretty)
          )
        )
      ),
      div(
        button(
          s"Streaming jsonl different origin",
          onClick --> (_ =>
            LocalEndpoints
              .allStream(())
              .jsonlEither[Organisation](localhost): organisation =>
                result.emit(organisation.toJsonPretty)
          )
        )
      ),
      p(
        s"Click the buttons below to make requests to the backend $httpbin."
      ),
      button(
        "runJs remote",
        onClick --> (_ => HttpBinEndpoints.get(()).run(httpbin))
      ),
      button(
        "emitTo",
        onClick --> (_ =>
          HttpBinEndpoints
            .get(())
            .emit(httpbin, eventBus)
        )
      )
    ),
    span(
      hr(),
      button(
        "WebSocket",
        onClick --> { _ =>
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

            _ <- result.zEmit("WebSocket closed.")

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
      onClick --> { _ =>
        queue.offer(WebSocketFrame.text("Hello from client!")).run
      }
    ),
    button(
      "Close WebSocket",
      onClick --> { _ =>
        queue.offer(WebSocketFrame.close).run
        closeWS.succeed(()).run
      }
    ),
    div(
      h3("WebSocket Messages:"),
      child <-- newMesageBus.events.map(msg => p(s"Sent: $msg"))
    ),
    div(
      h3("Responses:"),
      child <-- result.events
        .mergeWith(eventBus.events.map(_.toJsonPretty))
        .scanLeft("")((acc, next) => acc + "\n" + next)
        .map(text => pre(text))
    )
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
