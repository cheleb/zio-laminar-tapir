package demo.hello

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

import demo.BaseEndpoint

import sttp.capabilities.zio.ZioStreams
import demo.Organisation
import demo.GetResponse
import sttp.model.StatusCode
import zio.json.JsonCodec

case class ErrorResponse(message: String) derives JsonCodec

trait HelloEndpoints extends BaseEndpoint:

  val helloEndpoint = endpoint.get
    .in("hello")
    .out(stringBody)
    .description("A simple hello endpoint")

  val proxyEndpoint = endpoint.get
    .in("httpbin")
    .out(jsonBody[GetResponse])
    .errorOut(
      oneOf(
        oneOfVariant(
          statusCode(StatusCode.InternalServerError)
            .and(jsonBody[ErrorResponse])
        )
      )
    )
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

  val boomEndpoint = endpoint.get
    .in("boom")
    .in(query("n").description("The depth of the boom").example(5).default(0))
    .description("An endpoint that always fails with an error")
    .errorOut(
      oneOf(
        oneOfVariant(
          statusCode(StatusCode.InternalServerError)
            .and(jsonBody[ErrorResponse])
        )
      )
    )
