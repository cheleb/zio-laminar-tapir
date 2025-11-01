package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.*
import org.scalajs.dom
import sttp.model.Uri
import sttp.tapir.client.sttp4.ws.WebSocketSttpClientInterpreter

import zio.stream.ZStream
import sttp.client4.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp4.ws.zio.* // for zio

given httpbin: Uri = Uri.unsafeParse("https://httpbin.org")
given websocket: Uri = Uri.unsafeParse("https://echo.websocket.org")
val localhost = Uri.unsafeParse(dom.window.location.origin)

var result = EventBus[String]()

val myApp =
  val eventBus = new EventBus[GetResponse]()
//  val errorBus = new EventBus[Throwable]()
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
    div(
      hr(),
      button(
        "runJs WebSocket",
        onClick --> { _ =>
          val backend = FetchZioBackend()
          val client = WebSocketSttpClientInterpreter()
            .toClientThrowErrors(
              WebsocketEndpoint.wsEndpoint,
              Some(websocket),
              backend
            )
          client(())
            .flatMap { socket =>
              println("WebSocket connected")

              socket(ZStream("hello", "from", "websockets"))
                .runForeach(msg => ZIO.attempt(result.emit(s"Received: $msg")))

            }
            .catchAll(th =>
              ZIO.attempt {
                println("WebSocket connection failed: " + th.getMessage)
                result.emit(s"Failed")
              }
            )
            .run

//            .onComplete {
          //   case Success(value) =>

          //   case Failure(th) =>
          //     println("WebSocket connection failed: " + th.getMessage)
          //     ZIO.attempt(result.emit(s"Failed"))
          // }

        }
      )
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
