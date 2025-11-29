package demo

import zio.*

import sttp.tapir.server.ServerEndpoint
import dev.cheleb.ftapir.*
import sttp.tapir.ztapir.*

import sttp.capabilities.zio.ZioStreams
import zio.stream.ZStream

class OrganisationController private (
//    organisationService: OrganisationService,
//    jwtService: JWTService
) extends BaseController[ZioStreams, Task]:
  // extends SecuredBaseController[String, UserID](jwtService.verifyToken) {

  // val create: ServerEndpoint[Any, Task] =
  //   OrganisationEndpoint.create.zServerAuthenticatedLogic {
  //     userId => organisation =>
  //       organisationService.create(organisation, userId.id)
  //   }

  val listAll: ServerEndpoint[Any, Task] =
    OrganisationEndpoint.all.zServerLogic { _ =>
      // organisationService.listAll()
      ZIO.succeed(List())
    }

  val streamAll: ZServerEndpoint[Any, ZioStreams] =
    OrganisationEndpoint.allStream.zServerLogic { _ =>
      // organisationService.streamAll()
      ZIO.succeed(ZStream.empty)
    }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(
      // create,
      listAll
    )

  override def streamRoutes: List[ServerEndpoint[ZioStreams, Task]] = List(
    streamAll
  )

object OrganisationController {
//  def makeZIO: URIO[OrganisationService & JWTService, OrganisationController] =
  def makeZIO: URIO[Any, OrganisationController] =
    for _ <- ZIO.debug("Creating OrganisationController")
    //      organisationService <- ZIO.service[OrganisationService]
    //      jwtService <- ZIO.service[JWTService]
    yield new OrganisationController(
      // organisationService, jwtService
    )

}
