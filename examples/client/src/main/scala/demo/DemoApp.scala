package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.*
import org.scalajs.dom
import sttp.model.Uri

val httpbin = Uri.unsafeParse("https://httpbin.org")
val localhost = Uri.unsafeParse(dom.window.location.origin)

var result = Var[List[String]](List.empty)

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
              .jsonl[Organisation](organisation =>
                result.update(strs => strs :+ organisation.toJsonPretty)
              )
          )
        )
      ),
      div(
        button(
          s"Streaming jsonl different origin",
          onClick --> (_ =>
            LocalEndpoints.allStream
              .on(localhost)(())
              .jsonl[Organisation](organisation =>
                result.update(strs => strs :+ organisation.toJsonPretty)
              )
          )
        )
      ),
      p(
        s"Click the buttons below to make requests to the backend $httpbin."
      ),
      button(
        "runJs remote",
        onClick --> (_ => HttpBinEndpoints.get.on(httpbin)(()).runJs)
      ),
      button(
        "emitTo",
        onClick --> (_ =>
          HttpBinEndpoints.get
            .on(httpbin)(())
            .emitTo(eventBus)
        )
      )
    ),
    div(
      h3("Responses:"),
      children <-- result.signal.map(strs => strs.map(str => div(str)))
    )
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
