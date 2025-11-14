package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*

import org.scalajs.dom
import sttp.model.Uri

import io.github.nguyenyou.webawesome.laminar.*

val httpbin: Uri = Uri.unsafeParse("https://httpbin.org")

val localhost = Uri.unsafeParse(dom.window.location.origin)

var result = EventBus[String]()

val myApp =
  val eventBus = new EventBus[GetResponse]()
  val organisationBus = new EventBus[Organisation]()
  val clear = EventBus[String]()
  div(
    h1("ZIO Laminar Tapir Demo Client"),
    div(
      // Make the TabGroup and the Responses panel appear side by side
      styleAttr := "display: flex; align-items: flex-start; gap: 1rem; w",
      TabGroup(_.placement.start, _.active := "websocket")(
        Tab(_.panel := "session")("Session management"),
        Tab(_.panel := "batchSameOrigin")("Same Origin"),
        Tab(_.panel := "batchDifferentOrigin")("Different Origin"),
        Tab(_.panel := "streamingSameOrigin")("Streaming Same Origin"),
        Tab(_.panel := "streamingDifferentOrigin")(
          "Streaming Different Origin"
        ),
        Tab(_.panel := "websocket")("WebSocket"),
        TabPanel(_.name := "session")(
          sessionManagement()
        ),
        TabPanel(_.name := "batchSameOrigin")(
          batch.sameOrigin(organisationBus)
        ),
        TabPanel(_.name := "batchDifferentOrigin")(
          batch.differentOrigin(eventBus)
        ),
        TabPanel(_.name := "streamingSameOrigin")(streamingSameOrigin),
        TabPanel(_.name := "streamingDifferentOrigin")(
          streamingDifferentOrigin
        ),
        TabPanel(_.name := "websocket")(websocket)
      ),
      div(
        styleAttr := "margin-left: 1rem; width: 40%;",
        Textarea(
          _.cols := 100,
          _.rows := 20,
          _.value <-- result.events
            .mergeWith(eventBus.events.map(_.toJsonPretty))
            .mergeWith(organisationBus.events.map(_.toJsonPretty))
            .mergeWith(clear.events)
            .scanLeft("") { (acc, next) =>
              if next == "" then ""
              else acc + "\n" + next
            },
          _.value <-- clear.events.mapTo("")
        )(
        ),
        Button()(
          "Clear Responses",
          onClick.mapTo("") --> clear
        )
      )
    ),
    hr(),
    a(
      href := "https://github.com/cheleb/zio-laminar-tapir",
      "GitHub Repository"
    )
  )

@main
def main =
  val containerNode = dom.document.getElementById("app")
  render(containerNode, myApp)
