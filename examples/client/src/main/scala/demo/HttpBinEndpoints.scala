package demo

import sttp.tapir.*

import zio.stream.*
import zio.json.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import sttp.capabilities.zio.ZioStreams
import java.util.UUID

case class GetResponse(args: Map[String, String]) derives JsonCodec

case class LatLon(lat: Double, lon: Double) derives JsonCodec, Schema

case class Organisation(
    id: UUID,
    name: String,
    location: Option[LatLon]
) derives JsonCodec,
      Schema

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

  val allStream
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in("api" / "organisation" / "stream")
      .out(
        streamBody(ZioStreams)(
          summon[Schema[Organisation]],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")

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
