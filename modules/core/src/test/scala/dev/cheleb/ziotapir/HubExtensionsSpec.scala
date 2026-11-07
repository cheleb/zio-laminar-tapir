package dev.cheleb.ziotapir

import zio.*
import zio.test.*

import sttp.ws.WebSocketFrame

object HubExtensionsSpec extends ZIOSpecDefault {

  def spec = suite("Hub[WebSocketFrame] extensions")(
    test("sendText publishes a text frame") {
      ZIO.scoped {
        for
          hub <- Hub.unbounded[WebSocketFrame]
          queue <- hub.subscribe
          published <- hub.sendText("hello")
          frame <- queue.take
        yield assertTrue(published, frame == WebSocketFrame.text("hello"))
      }
    },
    test("closeGracefully publishes a close frame") {
      ZIO.scoped {
        for
          hub <- Hub.unbounded[WebSocketFrame]
          queue <- hub.subscribe
          taker <- queue.take.fork
          // Wait until the subscriber is blocked on take, so the published
          // frame is delivered to it before the hub is shut down.
          _ <- taker.status.repeatUntil(_.isSuspended)
          _ <- hub.closeGracefully
          frame <- taker.join
        yield assertTrue(frame == WebSocketFrame.Close(1000, "Normal Closure"))
      }
    },
    test("closeGracefully shuts the hub down") {
      for
        hub <- Hub.unbounded[WebSocketFrame]
        _ <- hub.closeGracefully
        isShutdown <- hub.isShutdown
      yield assertTrue(isShutdown)
    },
    test("runSyncUnsafe runs an effect and returns its value") {
      assertTrue(ZIO.succeed(42).runSyncUnsafe() == 42)
    }
  )
}
