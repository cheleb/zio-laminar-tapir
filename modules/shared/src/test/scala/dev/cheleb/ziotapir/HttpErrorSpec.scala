package dev.cheleb.ziotapir

import zio.test.*

import sttp.model.StatusCode

object HttpErrorSpec extends ZIOSpecDefault {

  def spec = suite("HttpError")(
    test("decode builds an HttpError from a status code and a message") {
      val error = HttpError.decode(StatusCode.NotFound -> "Not here")
      assertTrue(
        error.statusCode == StatusCode.NotFound,
        error.message == "Not here",
        error.getMessage == "Not here",
        error.cause.getMessage == "Not here"
      )
    },
    test("encode maps any throwable to an internal server error") {
      val encoded = HttpError.encode(new IllegalStateException("Boom"))
      assertTrue(encoded == (StatusCode.InternalServerError -> "Boom"))
    },
    test("encode then decode round trips the message") {
      val decoded = HttpError.decode(HttpError.encode(new Exception("Oops")))
      assertTrue(
        decoded.statusCode == StatusCode.InternalServerError,
        decoded.message == "Oops"
      )
    }
  )
}
