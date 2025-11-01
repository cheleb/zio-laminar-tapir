package dev.cheleb.ziotapir

import zio.*
import sttp.ws.WebSocketFrame

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)

/** Extensions for `Hub[WebSocketFrame]`.
  */
extension (hub: Hub[WebSocketFrame])

  /** Sends a text message over the WebSocket.
    *
    * @param message
    * @return
    *   A `UIO[Boolean]` indicating whether the message was successfully
    *   published.
    */
  def sendText(message: String): UIO[Boolean] =
    hub.publish(WebSocketFrame.text(message))

  /** Closes the WebSocket connection gracefully by sending a Close frame and
    * shutting down the hub.
    *
    * @return
    *   A `UIO[Unit]` that completes when the close operation is done.
    */
  def closeGracefully: UIO[Unit] =
    ZIO.uninterruptible:
      for
        _ <- hub.publish(WebSocketFrame.Close(1000, "Normal Closure"))
        _ <- hub.shutdown
      yield ()
