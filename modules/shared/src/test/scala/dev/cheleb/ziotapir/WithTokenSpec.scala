package dev.cheleb.ziotapir

import zio.test.*

import dev.cheleb.ziojwt.WithToken

object WithTokenSpec extends ZIOSpecDefault {

  private final case class Token(token: String, expiration: Option[Long])
      extends WithToken

  private def nowSeconds = System.currentTimeMillis() / 1000

  def spec = suite("WithToken")(
    test("a token without expiration never expires") {
      assertTrue(!Token("t", None).expired)
    },
    test("a token expiring in the future is not expired") {
      assertTrue(!Token("t", Some(nowSeconds + 3600)).expired)
    },
    test("a token that expired in the past is expired") {
      assertTrue(Token("t", Some(nowSeconds - 3600)).expired)
    }
  )
}
