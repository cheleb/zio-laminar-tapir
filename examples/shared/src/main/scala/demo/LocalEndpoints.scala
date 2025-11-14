package demo

import zio.json.*
import zio.stream.*

import java.util.UUID

import sttp.capabilities.zio.ZioStreams

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

case class LatLon(lat: Double, lon: Double) derives JsonCodec, Schema

case class Organisation(
    id: UUID,
    name: String,
    location: Option[LatLon]
) derives JsonCodec,
      Schema

object LocalEndpoints extends BaseEndpoint {

  /** A simple endpoint returning a famous place as JSON
    */
  val aPlace: Endpoint[Unit, Unit, Throwable, Organisation, Any] =
    baseEndpoint
      .tag("Admin")
      .name("a famous place")
      .get
      .in("zio-laminar-tapir" / "demo" / "famous-place.json")
      .out(jsonBody[Organisation])
      .description("Get a famous place")

  /** An endpoint streaming all organisations as JSONL
    */
  val allStream
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in("zio-laminar-tapir" / "demo" / "famous-places.jsonl")
      .out(
        streamBody(ZioStreams)(
          summon[Schema[Organisation]],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")

}
