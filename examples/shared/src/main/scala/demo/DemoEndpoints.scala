package demo

import io.circe.Codec

import zio.json.*
import zio.stream.*

import java.util.UUID

import sttp.capabilities.zio.ZioStreams

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio as tapirZIO
import sttp.tapir.json.circe as tapirCirce

case class LatLon(lat: Double, lon: Double) derives JsonCodec, Codec, Schema

case class Organisation(
    id: UUID,
    name: String,
    location: Option[LatLon]
) derives JsonCodec,
      Codec,
      Schema

object DemoEndpoints extends BaseEndpoint {

  /** A simple endpoint returning a famous place as JSON
    */
  val aPlaceZio: Endpoint[Unit, Unit, Throwable, Organisation, Any] =
    baseEndpoint
      .tag("Admin")
      .name("a famous place")
      .get
      .in("zio-laminar-tapir" / "demo" / "famous-place.json")
      .out(tapirZIO.jsonBody[Organisation])
      .description("Get a famous place")

  val aPlaceCirce: Endpoint[Unit, Unit, Throwable, Organisation, Any] =
    baseEndpoint
      .tag("Admin")
      .name("a famous place")
      .get
      .in("zio-laminar-tapir" / "demo" / "famous-place.json")
      .out(tapirCirce.jsonBody[Organisation])
      .description("Get a famous place")

  /** An endpoint streaming all organisations as JSONL
    */
  val organisations: Endpoint[Unit, Unit, Throwable, ZStream[
    Any,
    Throwable,
    Byte
  ], ZioStreams] =
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

//https://raw.githubusercontent.com/cheleb/zio-laminar-tapir/refs/heads/master/examples/client/famous-places.jsonl
  val organisationsRawGithub
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in(
        "cheleb" / "zio-laminar-tapir" / "refs" / "heads" / "master" / "examples" / "client" / "famous-places.jsonl"
      )
      .out(
        streamBody(ZioStreams)(
          summon[Schema[Organisation]],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")

}
