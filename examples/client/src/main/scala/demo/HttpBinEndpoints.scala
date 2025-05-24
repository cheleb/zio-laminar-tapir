package demo

import zio.json.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

case class GetResponse(args: Map[String, String]) derives JsonCodec

trait BaseEndpoint {
  val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val baseSecuredEndpoint: Endpoint[String, Unit, Throwable, Unit, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())

}

object HttpBinEndpoints extends BaseEndpoint {
  val get = baseEndpoint.get.in("get").out(jsonBody[GetResponse])
  val getInt =
    baseEndpoint.get.in("get").in(query[Int]("int")).out(jsonBody[GetResponse])

}

final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable
) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)) =
    HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))
  def encode(error: Throwable) =
    error match
      // case UnauthorizedException(msg) => (StatusCode.Unauthorized, msg)
      // case TooYoungException(_) => (StatusCode.BadRequest, error.getMessage())
      // case InvalidCredentialsException() =>
      //   (StatusCode.Unauthorized, error.getMessage())
      // case UserAlreadyExistsException() =>
      //   (StatusCode.Conflict, error.getMessage())
      case _ => (StatusCode.InternalServerError, error.getMessage())
}
