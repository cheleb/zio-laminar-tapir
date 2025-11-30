package demo.cats

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.*

import fs2.{Chunk, Stream}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.HeaderNames
import sttp.tapir.*

import sttp.tapir.server.http4s.Http4sServerInterpreter

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.*
import demo.Organisation
import java.util.UUID
import demo.LatLon
import demo.OrganisationEndpoint

// https://github.com/softwaremill/tapir/issues/367
object StreamingHttp4sFs2Server extends IOApp:
  // corresponds to: GET /receive?name=...
  // We need to provide both the schema of the value (for documentation), as well as the format (media type) of the
  // body. Here, the schema is a `string` (set by `streamTextBody`) and the media type is `text/plain`.
  val streamingEndpoint
      : PublicEndpoint[Unit, Unit, (Long, Stream[IO, Byte]), Fs2Streams[IO]] =
    endpoint.get
      .in("receive")
      .out(header[Long](HeaderNames.ContentLength))
      .out(
        streamTextBody(Fs2Streams[IO])(
          CodecFormat.TextPlain(),
          Some(StandardCharsets.UTF_8)
        )
      )

  // converting an endpoint to a route (providing server-side logic)
  val streamingRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      streamingEndpoint.serverLogicSuccess { _ =>
        val size = 100L
        Stream
          .emit(List[Char]('a', 'b', 'c', 'd'))
          .repeat
          .flatMap(list => Stream.chunk(Chunk.from(list)))
          .metered[IO](100.millis)
          .take(size)
          .covary[IO]
          .map(_.toByte)
          .pure[IO]
          .map(s => (size, s))
      }
    )
  // val batchEndpoint = endpoint.get
  //   .in("organisations")
  //   .out(jsonBody[List[Organisation]])

  val batchRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      OrganisationEndpoint.all.serverLogicSuccess { _ =>
        IO.pure(
          List(
            Organisation(
              UUID.randomUUID(),
              "Montpellier",
              Some(LatLon(43.6119, 3.8772))
            )
          )
        )
      }
    )
  import cats.syntax.semigroupk.*
  override def run(args: List[String]): IO[ExitCode] =
    // starting the server
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> (streamingRoutes <+> batchRoutes)).orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
