package demo

import zio.*
import zio.json.*
import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import io.github.nguyenyou.webawesome.laminar.*

def streamingSameOrigin =
  div(
    Button()(
      s"Streaming jsonl sameorigin",
      onClick --> (_ =>
        LocalEndpoints
          .allStream(())
          .jsonlEither[Organisation]: organisation =>
            result.emit(organisation.toJsonPretty)
      )
    )
  )

def streamingDifferentOrigin =
  div(
    Button()(
      s"Streaming jsonl differentorigin ($localhost)",
      onClick --> (_ =>
        LocalEndpoints
          .allStream(())
          .jsonlEither[Organisation](localhost): organisation =>
            result.emit(organisation.toJsonPretty)
      )
    )
  )
