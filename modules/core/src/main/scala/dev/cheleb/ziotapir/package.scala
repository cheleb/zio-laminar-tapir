package dev.cheleb.ziotapir

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams

/** Type alias for ZIO streams with websockets.
  *
  * Needed for Tag (izumi.reflect.TagK) to work properly.
  */
type ZioStreamsWithWebSockets = ZioStreams & WebSockets

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)
