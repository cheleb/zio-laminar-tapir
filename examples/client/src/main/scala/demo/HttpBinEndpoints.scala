package demo

import zio.json.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

import dev.cheleb.ziotapir.HttpError
case class GetResponse(args: Map[String, String], headers: Map[String, String])
    derives JsonCodec

trait BaseEndpoint {
  val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(
      HttpError.encode
    )

  val baseSecuredEndpoint: Endpoint[String, Unit, Throwable, Unit, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())

}

object HttpBinEndpoints extends BaseEndpoint {
  val get: Endpoint[Unit, Unit, Throwable, GetResponse, Any] =
    baseEndpoint.get.in("get").out(jsonBody[GetResponse])
  val getInt: Endpoint[Unit, Int, Throwable, GetResponse, Any] =
    baseEndpoint.get.in("get").in(query[Int]("int")).out(jsonBody[GetResponse])

}
