package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import io.github.nguyenyou.webawesome.laminar.*

object batch:
  def sameOrigin =
    Button(_.variant.brand)(
      "Streaming jsonl sameorigin",
      onClick --> (_ =>
        LocalEndpoints
          .allStream(())
          .jsonlEither[Organisation]: organisation =>
            result.emit(organisation.toJsonPretty)
      )
    )

  def differentOrigin(eventBus: EventBus[GetResponse]) =
    div(
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
    )
