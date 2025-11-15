package demo

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import facades.highlightjs.hljs
import io.github.nguyenyou.webawesome.laminar.*

object batch:
  def sameOrigin(eventBus: EventBus[Organisation]) =
    div(
      cls := "spaced",
      Button(_.variant.brand)(
        "Batch json (sameorigin)",
        onClick --> (_ =>
          LocalEndpoints
            .aPlace(())
            .emit(eventBus)
        )
      ),
      div(
        "As easy as:",
        pre(
          code(
            className := "language-scala",
            """
                | LocalEndpoints
                |            .aPlace(())
                |            .emit(eventBus)
                """.stripMargin,
            onMountCallback(ctx => hljs.highlightElement(ctx.thisNode.ref))
          )
        )
      )
    )

  def differentOrigin(eventBus: EventBus[GetResponse]) =
    div(
      cls := "spaced",
      p(
        s"Click the buttons below to make requests to the backend $httpbin."
      ),
      div(
        Button()(
          "runJs remote",
          onClick --> (_ => HttpBinEndpoints.get(()).run(httpbin))
        ),
        label("Fire and Forget")
      ),
      Button()(
        "emitTo",
        onClick --> (_ =>
          HttpBinEndpoints
            .get(())
            .emit(httpbin, eventBus)
        )
      )
    )
