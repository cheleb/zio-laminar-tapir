package dev.cheleb.ziotapir

import zio.*
import zio.stream.*

import zio.test.*

import sttp.capabilities.zio.ZioStreams
import sttp.client4.GenericRequest
import sttp.client4.impl.zio.RIOMonadAsyncError
import sttp.client4.testing.WebSocketStreamBackendStub
import sttp.model.StatusCode
import sttp.model.Uri
import sttp.tapir.*
import sttp.tapir.client.sttp4.SttpClientInterpreter
import sttp.tapir.client.sttp4.stream.StreamSttpClientInterpreter
import sttp.tapir.client.sttp4.ws.WebSocketSttpClientInterpreter

object BackendClientSpec extends ZIOSpecDefault {

  private val baseUrl = Uri.unsafeParse("http://backend.test:9999")

  private val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] =
    endpoint
      .errorOut(statusCode.and(plainBody[String]))
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  private val hello: Endpoint[Unit, String, Throwable, String, Any] =
    baseEndpoint.get.in("hello").in(query[String]("name")).out(stringBody)

  private val failing: Endpoint[Unit, Unit, Throwable, String, Any] =
    baseEndpoint.get.in("failing").out(stringBody)

  private val secured: Endpoint[String, Unit, Throwable, String, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .get
      .in("secured")
      .out(stringBody)

  private val streaming
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint.get
      .in("stream")
      .out(streamBody(ZioStreams)(Schema.string, CodecFormat.TextPlain()))

  private val securedStreaming
      : Endpoint[String, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
      .get
      .in("secured-stream")
      .out(streamBody(ZioStreams)(Schema.string, CodecFormat.TextPlain()))

  private def path(request: GenericRequest[?, ?]) = request.uri.path.toList

  private def bearer(request: GenericRequest[?, ?]) =
    request.headers
      .find(_.name.equalsIgnoreCase("Authorization"))
      .map(_.value)

  private val stub =
    WebSocketStreamBackendStub[Task, ZioStreams](new RIOMonadAsyncError[Any])
      .whenRequestMatches(r =>
        r.uri.host.contains("backend.test") && path(r) == List("hello")
      )
      .thenRespondF(r =>
        ZIO.succeed(
          sttp.client4.testing.ResponseStub
            .adjust(s"Hello ${r.uri.params.get("name").getOrElse("?")}")
        )
      )
      .whenRequestMatches(r => path(r) == List("failing"))
      .thenRespondAdjust("Teapot error", StatusCode(418))
      .whenRequestMatches(r =>
        path(r) == List("secured") && bearer(r).contains("Bearer s3cr3t")
      )
      .thenRespondAdjust("Welcome")
      .whenRequestMatches(r => path(r) == List("secured"))
      .thenRespondAdjust("Forbidden", StatusCode.Unauthorized)
      .whenRequestMatches(r => path(r) == List("stream"))
      .thenRespondAdjust(ZStream.fromIterable("line1\nline2".getBytes))
      .whenRequestMatches(r =>
        path(r) == List("secured-stream") && bearer(r).contains("Bearer s3cr3t")
      )
      .thenRespondAdjust(ZStream.fromIterable("secret".getBytes))

  private val clientLayer: ULayer[BackendClient] =
    ZLayer.succeed(
      BackendClientLive(
        stub,
        SttpClientInterpreter(),
        StreamSttpClientInterpreter(),
        WebSocketSttpClientInterpreter(),
        BackendClientConfig(baseUrl)
      ): BackendClient
    )

  private def asString(stream: Stream[Throwable, Byte]) =
    stream.runCollect.map(bytes => new String(bytes.toArray))

  def spec = suite("BackendClient")(
    suite("BackendClientConfig")(
      test("exposes the base url as an option") {
        assertTrue(
          BackendClientConfig(baseUrl).baseUrlAsOption.contains(baseUrl)
        )
      }
    ),
    suite("websocketUri")(
      test("maps http schemes to websocket schemes") {
        def ws(uri: String) =
          BackendClientLive.websocketUri(Uri.unsafeParse(uri)).toString
        assertTrue(
          ws("http://host:8080/api") == "ws://host:8080/api",
          ws("https://host/api") == "wss://host/api",
          ws("HTTPS://host") == "wss://host",
          ws("ws://host") == "ws://host",
          ws("wss://host") == "wss://host"
        )
      }
    ),
    suite("unsecured endpoint")(
      test("sends the payload to the configured base url") {
        for res <- hello("ZIO")
        yield assertTrue(res == "Hello ZIO")
      },
      test("maps error responses through the endpoint error output") {
        for exit <- failing(()).exit
        yield assertTrue(exit match
          case Exit.Failure(cause) =>
            cause.failureOption.exists {
              case HttpError(code, message, _) =>
                code == StatusCode(418) && message == "Teapot error"
              case _ => false
            }
          case _ => false)
      }
    ),
    suite("secured endpoint")(
      test("fails with RestrictedEndpointException when there is no token") {
        given Session[TestToken] = TestSession(baseUrl)
        for exit <- secured(()).exit
        yield assertTrue(
          exit.causeOption.flatMap(_.failureOption) ==
            Some(RestrictedEndpointException("No token found"))
        )
      },
      test("ignores tokens issued by another backend") {
        val session = TestSession(baseUrl)
        session.saveToken(Uri.unsafeParse("http://other:1234"), "s3cr3t")
        given Session[TestToken] = session
        for exit <- secured(()).exit
        yield assertTrue(exit.isFailure)
      },
      test("sends the session token as a bearer") {
        val session = TestSession(baseUrl)
        session.saveToken("s3cr3t")
        given Session[TestToken] = session
        for res <- secured(())
        yield assertTrue(res == "Welcome")
      },
      test("propagates server side rejection") {
        val session = TestSession(baseUrl)
        session.saveToken("wrong")
        given Session[TestToken] = session
        for exit <- secured(()).exit
        yield assertTrue(exit.causeOption.flatMap(_.failureOption).exists {
          case HttpError(code, _, _) => code == StatusCode.Unauthorized
          case _                     => false
        })
      }
    ),
    suite("stream endpoint")(
      test("returns the response body as a stream") {
        for
          stream <- streaming(())
          body <- asString(stream)
        yield assertTrue(body == "line1\nline2")
      },
      test("secured stream sends the session token") {
        val session = TestSession(baseUrl)
        session.saveToken("s3cr3t")
        given Session[TestToken] = session
        for
          stream <- securedStreaming(())
          body <- asString(stream)
        yield assertTrue(body == "secret")
      },
      test("secured stream fails without token") {
        given Session[TestToken] = TestSession(baseUrl)
        for exit <- securedStreaming(()).exit
        yield assertTrue(exit.isFailure)
      }
    )
  ).provideLayerShared(clientLayer)
}
