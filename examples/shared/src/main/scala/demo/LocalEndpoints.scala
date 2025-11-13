package demo

import zio.json.*
import zio.stream.*

import java.util.UUID

import sttp.capabilities.zio.ZioStreams

import sttp.tapir.*
import sttp.tapir.generic.auto.*
//import sttp.tapir.json.zio.*

case class LatLon(lat: Double, lon: Double) derives JsonCodec, Schema

case class Organisation(
    id: UUID,
    name: String,
    location: Option[LatLon]
) derives JsonCodec,
      Schema

object LocalEndpoints extends BaseEndpoint {

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
