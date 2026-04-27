package demo.hello

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

import demo.BaseEndpoint

import sttp.capabilities.zio.ZioStreams
import demo.Organisation
import demo.GetResponse

trait HelloEndpoints extends BaseEndpoint:

  val helloEndpoint = endpoint.get
    .in("hello")
    .out(stringBody)
    .description("A simple hello endpoint")

  val proxyEndpoint = endpoint.get
    .in("httpbin")
    .out(jsonBody[GetResponse])
    .description("A simple proxy endpoint")

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
