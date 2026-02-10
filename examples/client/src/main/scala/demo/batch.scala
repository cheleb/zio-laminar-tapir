package demo

import zio.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import dev.cheleb.ziotapir.awesomelaminar.*

import io.github.nguyenyou.webawesome.laminar.*

object batch:
  def sameOrigin(eventBus: EventBus[Organisation]) =
    div(
      cls := "spaced",
      Button(_.variant := "brand")(
        "Batch json (sameorigin)",
        onClick --> (_ =>
          DemoEndpoints
            .aPlace(())
            .emit(eventBus)
        )
      )
    )
      .withSnippet:
        """// Simple as that:
         | DemoEndpoints
         |            .aPlace(())
         |            .emit(eventBus)
    """

  def differentOrigin(eventBus: EventBus[GetResponse]) =
    val showMessage = Var("")
    div(
      cls := "spaced",
      p(
        s"Click the buttons below to make requests to the backend $httpbin."
      ),
      div(
        ZButton("run", "fire", httpbin):
          for
            _ <- HttpBinEndpoints.get(())
            _ = showMessage.set(
              "Request sent to httpbin.org (check network tab in devtools)"
            )
            _ <- ZIO.sleep(3.seconds)
            _ = showMessage.set("")
          yield ()
      ).withSnippet:
        """|val httpbin: Uri = Uri.unsafeParse("https://httpbin.org")
           |// Run request and get response
           | HttpBinEndpoints.get(()).run(httpbin)
        """
      ,
      child.maybe <-- showMessage.signal.map:
        case ""      => None
        case message =>
          Some(
            Callout(
              _.slots.icon(Icon(_.name := "circle-info")())
            )(
              message
            )
          )
      ,
      Button()(
        "emitTo",
        onClick --> (_ =>
          HttpBinEndpoints
            .get(())
            .emit(httpbin, eventBus)
        )
      )
    )
      .withSnippet:
        """
  |val httpbin: Uri = Uri.unsafeParse("https://httpbin.org")
  |// Run request and get response
  | HttpBinEndpoints.get(()).emit(httpbin, eventBus)
  |
  """
