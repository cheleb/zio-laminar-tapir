package demo

import org.scalajs.dom
import com.raquo.laminar.api.L.*

import dev.cheleb.ziolaminartapir.*
import sttp.model.Uri

val z = Uri.unsafeParse("https://httpbin.org")

val myApp =

  val eventBus = new EventBus[GetResponse]()
  val errorBus = new EventBus[Throwable]()

  div(
    h1("Hello, world!"),
    p("This is a simple example of a Laminar app using ZIO and Tapir."),
    p(
      s"Click the buttons below to make requests to the backend ${BackendClientLive.developmentApiServer}."
    ),
    button(
      "runJs",
      onClick --> (_ => HttpBinEndpoints.get(()).runJs(z, errorBus))
    ),
    button(
      "emitTo",
      onClick --> (_ => HttpBinEndpoints.get(()).emitTo(z, eventBus))
    )
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
