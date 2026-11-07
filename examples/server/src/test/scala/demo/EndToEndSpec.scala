package demo

import zio.*
import zio.http.Server
import zio.json.*
import zio.stream.*
import zio.test.*

import java.net.ServerSocket
import java.util.UUID

import demo.hello.HelloEndpoints
import demo.hello.HelloService
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.client.ZIOSttpBackendLive
import dev.cheleb.ziotapir.server.*
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.*
import sttp.ws.WebSocketFrame

/** End to end tests: the example server ([[HttpApi]]) is started on a free
  * port, together with a few test controllers, and is called through the
  * library client ([[ZIOSttpBackendLive]] and the endpoint extensions).
  */
object EndToEndSpec extends ZIOSpecDefault {

  /** The example endpoints have a `Unit` error output, the library extensions
    * need a `Throwable` one.
    */
  extension [I, O, R](e: Endpoint[Unit, I, Unit, O, R])
    private def throwing: Endpoint[Unit, I, Throwable, O, R] =
      e.mapErrorOut[Throwable](_ => RuntimeException("Request failed"))(_ => ())

  private object Hello extends HelloEndpoints

  private val places = (1 to 20).toList.map(i =>
    Organisation(UUID.randomUUID(), s"Place $i é", Some(LatLon(i, -i)))
  )

  /** Serves the external endpoints used by the example client (a fake
    * httpbin, and the famous places), so that the tests are self contained.
    */
  private class FakeExternalController extends BaseController[ZioStreams] {

    /** Serves both [[HttpBinEndpoints.get]] and [[HttpBinEndpoints.getInt]].
      */
    val get: ServerEndpoint[Any, Task] =
      HttpBinEndpoints.baseEndpoint.get
        .in("get")
        .in(query[Option[Int]]("int"))
        .out(jsonBody[GetResponse])
        .zServerLogic {
          case Some(i) if i < 0 =>
            ZIO.fail(IllegalArgumentException(s"Negative: $i"))
          case Some(i) =>
            ZIO.succeed(GetResponse(Map("int" -> i.toString), Map.empty))
          case None =>
            ZIO.succeed(GetResponse(Map.empty, Map("X-Fake" -> "httpbin")))
        }

    val aPlace: ServerEndpoint[Any, Task] =
      DemoEndpoints.aPlace.zServerLogic(_ => ZIO.succeed(places.head))

    val organisations: ServerEndpoint[ZioStreams, Task] =
      DemoEndpoints.organisations.zServerLogic(_ =>
        ZIO.succeed(ZStream.fromIterable(places).toJsonLinesStream)
      )

    override def routes = List(get, aPlace)
    override def streamRoutes = List(organisations)
  }

  final case class User(name: String)

  private object SecuredEndpoints extends BaseEndpoint {
    val me: Endpoint[String, Unit, Throwable, String, Any] =
      baseSecuredEndpoint.get.in("me").out(stringBody)
  }

  private class MeController
      extends SecuredBaseController[String, User, ZioStreams](token =>
        if token == "s3cr3t" then ZIO.succeed(User("Alice"))
        else ZIO.fail(RestrictedEndpointException("Invalid token"))
      ) {
    override def routes =
      List(SecuredEndpoints.me.zServerAuthenticatedLogic { user => _ =>
        ZIO.succeed(s"Hello ${user.name}")
      })
  }

  final case class TestToken(token: String) extends WithToken {
    def expiration: Option[Long] = None
  }

  private class TestSession(issuer: Uri, initial: Option[String])
      extends Session[TestToken] {
    private var current = initial.map(TestToken(_))
    def isActive = current.isDefined
    def saveToken(issuer: Uri, token: String) = current = Some(TestToken(token))
    def saveToken(token: String) = saveToken(issuer, token)
    def getToken(i: Uri) = current.filter(_ => i == issuer)
    def loadUserState() = ()
    def loadUserState(issuer: Uri) = ()
    def clearUserState() = current = None
  }

  private val freePort: Int =
    val socket = ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()

  private val baseUri = Uri.unsafeParse(s"http://localhost:$freePort")

  private val clientLayer: TaskLayer[BackendClient] =
    ZIOSttpBackendLive.configuredLayerOn(baseUri)

  /** Starts the example API, plus the test controllers. The HelloService
    * proxies to the fake httpbin served by the same server.
    */
  private val serverLayer: TaskLayer[Server] =
    ZLayer.make[Server](
      Server.defaultWithPort(freePort),
      HelloService.live,
      clientLayer,
      ZLayer.scoped(
        for
          apiEndpoints <- HttpApi.endpoints
          extra = List(FakeExternalController(), MeController())
          extraEndpoints = extra.flatMap(c => c.routes ++ c.streamRoutes)
          all: List[ServerEndpoint[ZioStreams & WebSockets, Task]] =
            apiEndpoints ++ extraEndpoints
          _ <- Server.install(ZioHttpInterpreter().toHttp(all))
        yield ()
      )
    )

  private def failure(exit: Exit[Throwable, ?]) =
    exit.causeOption.flatMap(_.failureOption)

  def spec = suite("End to end")(
    suite("unsecured endpoints")(
      test("hello endpoint of the example HelloController") {
        for res <- Hello.helloEndpoint.throwing(())
        yield assertTrue(res == "Hello, World service!")
      },
      test("hello endpoint of the example WebSocketController") {
        for res <- WebsocketEndpoint.helloEndpoint.throwing(())
        yield assertTrue(res == "Hello, World!")
      },
      test("HelloService proxies to httpbin through the library client") {
        for res <- Hello.proxyEndpoint.throwing(())
        yield assertTrue(res.headers.get("X-Fake").contains("httpbin"))
      },
      test("JSON body with query parameter") {
        for res <- HttpBinEndpoints.getInt(42)
        yield assertTrue(res.args == Map("int" -> "42"))
      },
      test("JSON body") {
        for res <- DemoEndpoints.aPlace(())
        yield assertTrue(res == places.head)
      },
      test("server failures are decoded as HttpError") {
        for exit <- HttpBinEndpoints.getInt(-1).exit
        yield assertTrue(failure(exit).exists {
          case HttpError(code, message, _) =>
            code == StatusCode.InternalServerError && message == "Negative: -1"
          case _ => false
        })
      },
      test("unknown routes fail") {
        for exit <- WebsocketEndpoint.helloEndpoint
            .in("unknown")
            .throwing(())
            .exit
        yield assertTrue(exit.isFailure)
      }
    ),
    suite("secured endpoints")(
      test("succeeds with a valid token in the session") {
        given Session[TestToken] = TestSession(baseUri, Some("s3cr3t"))
        for res <- SecuredEndpoints.me(())
        yield assertTrue(res == "Hello Alice")
      },
      test("is rejected by the server with an invalid token") {
        given Session[TestToken] = TestSession(baseUri, Some("wrong"))
        for exit <- SecuredEndpoints.me(()).exit
        yield assertTrue(failure(exit).exists {
          case HttpError(_, message, _) => message == "Invalid token"
          case _                        => false
        })
      },
      test("fails on the client side without token") {
        given Session[TestToken] = TestSession(baseUri, None)
        for exit <- SecuredEndpoints.me(()).exit
        yield assertTrue(
          failure(exit).contains(RestrictedEndpointException("No token found"))
        )
      }
    ),
    suite("streaming endpoints")(
      test("JSONL stream is received and parsed") {
        for
          stream <- DemoEndpoints.organisations(())
          received <- stream
            .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
            .filter(_.nonEmpty)
            .map(_.fromJson[Organisation])
            .runCollect
        yield assertTrue(received.toList == places.map(Right(_)))
      },
      test("JSON array stream of the example HelloController") {
        for
          response <- HttpClientZioBackend.scoped().flatMap { backend =>
            basicRequest
              .get(baseUri.addPath("streaming"))
              .response(asStringAlways)
              .send(backend)
          }
          organisations = response.body.fromJson[List[Organisation]]
        yield assertTrue(
          response.code == StatusCode.Ok,
          organisations.map(_.size) == Right(100)
        )
      }
    ),
    suite("websocket endpoints")(
      test("echo endpoint echoes text frames") {
        for
          pipe <- WebsocketEndpoint.echo(())
          frames <- pipe(
            ZStream(WebSocketFrame.text("one"), WebSocketFrame.text("two"))
          ).collect { case WebSocketFrame.Text(payload, _, _) => payload }
            .take(2)
            .runCollect
            .timeoutFail(RuntimeException("timeout"))(10.seconds)
        yield assertTrue(frames.toList == List("ECHO: one", "ECHO: two"))
      },
      test("responseZIO exposes the upgrade response") {
        for
          response <- WebsocketEndpoint.echo.responseZIO(())
          frames <- response
            .body(ZStream(WebSocketFrame.text("ping")))
            .collect { case WebSocketFrame.Text(payload, _, _) => payload }
            .take(1)
            .runCollect
            .timeoutFail(RuntimeException("timeout"))(10.seconds)
        yield assertTrue(
          response.code == StatusCode.SwitchingProtocols,
          frames.toList == List("ECHO: ping")
        )
      }
    )
  ).provideSomeLayerShared[Scope](
    serverLayer >+> clientLayer // the server is shared by all tests
  ) @@ TestAspect.withLiveClock @@ TestAspect.sequential
}
