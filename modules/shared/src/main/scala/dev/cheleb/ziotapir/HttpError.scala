package dev.cheleb.ziotapir

import sttp.model.StatusCode

/** Represents an HTTP error with a status code, message, and cause.
  */
final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause)

/** Provides methods to decode and encode `HttpError` instances.
  */
object HttpError {
  def decode(tuple: (StatusCode, String)) =
    HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))
  def encode(error: Throwable) =
    error match
      case _ => (StatusCode.InternalServerError, error.getMessage())
}
