package dev.cheleb.ziotapir.client

import zio.*
import zio.test.*

import dev.cheleb.ziotapir.*

object ClientSpec extends ZIOSpecDefault {

  def spec = suite("client")(
    test("send provides the given BackendClient to the effect") {
      for
        backend <- ZIO.service[BackendClient]
        given BackendClient = backend
        same <- ZIO.serviceWith[BackendClient](_ eq backend).send
      yield assertTrue(same)
    },
    test("configuredLayerOn builds a client bound to the given uri") {
      for client <- ZIO.service[BackendClient]
      yield assertTrue(client match
        case live: BackendClientLive =>
          live.config.baseUrl.toString == "http://localhost:4242"
        case _ => false)
    }
  ).provideShared(
    ZIOSttpBackendLive.configuredLayerOn(
      sttp.model.Uri.unsafeParse("http://localhost:4242")
    )
  )
}
