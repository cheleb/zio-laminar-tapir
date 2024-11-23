package demo

import org.scalajs.dom
import com.raquo.laminar.api.L.*

import dev.cheleb.ziotapir.laminar.*
import sttp.model.Uri

val httpbin = Uri.unsafeParse("https://httpbin.org")

val myApp =
  val eventBus = new EventBus[GetResponse]()
//  val errorBus = new EventBus[Throwable]()

  div(
    h1("Hello, world!"),
    p("This is a simple example of a Laminar app using ZIO and Tapir."),
    p(
      s"Click the buttons below to make requests to the backend $httpbin."
    ),
    button(
      "runJs some origin",
      onClick --> (_ => HttpBinEndpoints.get(()).runJs)
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
