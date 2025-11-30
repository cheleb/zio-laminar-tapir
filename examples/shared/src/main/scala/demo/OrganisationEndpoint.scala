package demo

import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*

import sttp.capabilities.zio.ZioStreams

import zio.stream.*
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.IO

type JsonExtraction = [T] =>> EndpointIO.Body[String, T]

object OrganisationEndpoint extends BaseEndpoint:

  // val create: Endpoint[String, NewOrganisation, Throwable, Organisation, Any] =
  //   baseSecuredEndpoint
  //     .tag("Admin")
  //     .name("organisation")
  //     .post
  //     .in("organisation")
  //     .in(
  //       jsonBody[NewOrganisation]
  //         .description("Person to insert")
  //         .example(
  //           NewOrganisation(
  //             "EPFL",
  //             LatLon(46.519653, 6.632273),
  //             Mesh.default
  //           )
  //         )
  //     )
  //     .out(jsonBody[Organisation])
  //     .description("Create person")

  val all: PublicEndpoint[Unit, Throwable, List[Organisation], Any] =
    baseEndpoint
      .tag("Admin")
      .name("organisation")
      .get
      .in("organisation")
      .out(jsonBody[List[Organisation]])
      .description("Get all organisations")

  val allStream
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in("organisation" / "stream")
      .out(
        streamBody(ZioStreams)(
          Schema.derived[Organisation],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")

  val allStreamFs2
      : Endpoint[Unit, Unit, Throwable, fs2.Stream[IO, Byte], Fs2Streams[IO]] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in("organisation" / "stream")
      .out(
        streamBody(Fs2Streams[IO])(
          Schema.derived[Organisation],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")
