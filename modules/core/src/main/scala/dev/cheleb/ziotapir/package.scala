package dev.cheleb.ziotapir

import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

type ZioStreamsWithWebSockets = ZioStreams & WebSockets

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)
