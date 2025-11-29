package demo

import sttp.tapir.*
import sttp.tapir.generic.auto.*

import sttp.capabilities.zio.ZioStreams

import zio.stream.*

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

  def all(
      sjsonBody: JsonExtraction[List[Organisation]]
  ): PublicEndpoint[Unit, Throwable, List[Organisation], Any] =
    baseEndpoint
      .tag("Admin")
      .name("organisation")
      .get
      .in("organisation")
      .out(sjsonBody)
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
