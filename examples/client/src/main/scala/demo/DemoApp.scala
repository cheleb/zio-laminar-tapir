package demo

import org.scalajs.dom
import com.raquo.laminar.api.L.*

import dev.cheleb.ziotapir.laminar.*
import sttp.model.Uri

val httpbin = Uri.unsafeParse("https://httpbin.org")
val localhost = Uri.unsafeParse("http://localhost:8080")

val myApp =
  val eventBus = new EventBus[GetResponse]()
//  val errorBus = new EventBus[Throwable]()

  div(
    h1("ZIO and Tapir."),
    p("Same origin requests (will fail):"),
    button(
      "runJs some origin",
      onClick --> (_ => HttpBinEndpoints.get(()).runJs)
    ),
    p("Localhost requests (will fail):"),
    button(
      "runJs localhost",
      onClick --> (_ => HttpBinEndpoints.allStream.on(localhost)(()).runJs)
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
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
