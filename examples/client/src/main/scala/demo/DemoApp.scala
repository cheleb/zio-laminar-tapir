package demo

import org.scalajs.dom
import com.raquo.laminar.api.L.*

import dev.cheleb.ziolaminartapir.*
import sttp.model.Uri

val z = Uri.unsafeParse("https://httpbin.org")

val myApp =

  val eventBus = new EventBus[GetResponse]()

  div(
    h1("Hello, world!"),
    button(
      "runJs",
      onClick --> (_ => HttpBinEndpoints.get(()).runJsOn(z))
    ),
    button(
      "emitTo",
      onClick --> (_ => HttpBinEndpoints.get(()).emitToOn(eventBus, z))
    )
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
