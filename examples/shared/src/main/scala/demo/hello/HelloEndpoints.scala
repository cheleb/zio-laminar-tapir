package demo.hello

import sttp.tapir.*

import demo.BaseEndpoint

import sttp.capabilities.zio.ZioStreams
import demo.Organisation

trait HelloEndpoints extends BaseEndpoint:

  val helloEndpoint = endpoint.get
    .in("hello")
    .out(stringBody)
    .description("A simple hello endpoint")

  val streamingEndpoint = endpoint.get
    .in("streaming")
    .out(
      header[String]("Content-type")
    )
    .out(
      streamBody(ZioStreams)(
        summon[Schema[Organisation]],
        CodecFormat.TextEventStream()
      )
    )
    .description("A simple streaming endpoint that emits a stream of strings")
