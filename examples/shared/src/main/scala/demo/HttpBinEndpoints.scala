package demo

import io.circe.Codec

import zio.json.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio as tapirZIO
import sttp.tapir.json.circe as tapirCirce

import dev.cheleb.ziotapir.HttpError
case class GetResponse(args: Map[String, String], headers: Map[String, String])
    derives JsonCodec,
      Codec

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
    baseEndpoint.get.in("get").out(tapirZIO.jsonBody[GetResponse])
  val getInt: Endpoint[Unit, Int, Throwable, GetResponse, Any] =
    baseEndpoint.get
      .in("get")
      .in(query[Int]("int"))
      .out(tapirZIO.jsonBody[GetResponse])

  val getCirce: Endpoint[Unit, Unit, Throwable, GetResponse, Any] =
    baseEndpoint.get.in("get").out(tapirCirce.jsonBody[GetResponse])
  val getIntCirce: Endpoint[Unit, Int, Throwable, GetResponse, Any] =
    baseEndpoint.get
      .in("get")
      .in(query[Int]("int"))
      .out(tapirCirce.jsonBody[GetResponse])

}
