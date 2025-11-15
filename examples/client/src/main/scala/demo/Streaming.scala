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
        DemoEndpoints
          .organisations(())
          .jsonlEither[Organisation]: organisation =>
            result.emit(organisation.toJsonPretty)
      )
    )
  ).withSnippet:
    """|// Simple as that:
       |DemoEndpoints
       |   .organisations(())
       |   .jsonlEither[Organisation]: organisation =>
       |     result.emit(organisation.toJsonPretty)
         """

def streamingDifferentOrigin =
  div(
    Button()(
      s"Streaming jsonl differentorigin ($githubusercontent)",
      onClick --> (_ =>
        DemoEndpoints
          .organisationsRawGithub(())
          .jsonlEither[Organisation](githubusercontent): organisation =>
            result.emit(organisation.toJsonPretty)
      )
    )
  ).withSnippet:
    """|// Simple as that:
       |DemoEndpoints
       |   .organisationsRawGithub(())
       |   .jsonlEither[Organisation](githubusercontent): organisation =>
       |     result.emit(organisation.toJsonPretty)
         """
