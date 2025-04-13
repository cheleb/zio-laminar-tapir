package dev.cheleb.ziotapir

/** Typed exception for restricted endpoints.
  * @param message
  */
case class RestrictedEndpointException(message: String)
    extends RuntimeException(message)
