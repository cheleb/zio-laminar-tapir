package demo.hello

import sttp.tapir.*

import demo.BaseEndpoint

trait HelloEndpoints extends BaseEndpoint:

  val helloEndpoint = endpoint.get
    .in("hello")
    .out(stringBody)
    .description("A simple hello endpoint")
