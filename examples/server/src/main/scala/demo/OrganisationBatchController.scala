package demo

import zio.*
import zio.json.*

import sttp.tapir.server.ServerEndpoint
import dev.cheleb.ftapir.*
import sttp.tapir.ztapir.*

//import sttp.tapir.json.zio.jsonBody

import sttp.capabilities.zio.ZioStreams
import zio.stream.ZStream
import java.util.UUID

class OrganisationBatchController private[demo] (
//    organisationService: OrganisationService,
//    jwtService: JWTService
) extends BatchController[Task]:
  // extends SecuredBaseController[String, UserID](jwtService.verifyToken) {

  // val create: ServerEndpoint[Any, Task] =
  //   OrganisationEndpoint.create.zServerAuthenticatedLogic {
  //     userId => organisation =>
  //       organisationService.create(organisation, userId.id)
  //   }

  val listAll: ServerEndpoint[Any, Task] =
    OrganisationEndpoint.all.zServerLogic { _ =>
      // organisationService.listAll()
      ZIO.succeed(
        List(
          Organisation(
            UUID.randomUUID(),
            "EPFL",
            Some(LatLon(46.519653, 6.632273))
          )
        )
      )
    }

  override val batchRoutes: List[ServerEndpoint[Any, Task]] =
    List(
      // create,
      listAll
    )

class OrganisationStreamController private[demo] (
//    organisationService: OrganisationService,
//    jwtService: JWTService
) extends StreamController[ZioStreams, Task]:
  // extends SecuredBaseController[String, UserID](jwtService.verifyToken) {

  // val create: ServerEndpoint[Any, Task] =
  //   OrganisationEndpoint.create.zServerAuthenticatedLogic {
  //     userId => organisation =>
  //       organisationService.create(organisation, userId.id)
  //   }

  val streamAll: ZServerEndpoint[Any, ZioStreams] =
    OrganisationEndpoint.allStream.zServerLogic { _ =>
      // organisationService.streamAll()
      ZIO.succeed(
        ZStream.fromIterable(
          (Organisation(
            UUID.randomUUID(),
            "EPFL",
            Some(LatLon(46.519653, 6.632273))
          ).toJson + "\n").getBytes
        )
      )
    }

  override def streamRoutes: List[ServerEndpoint[ZioStreams, Task]] = List(
    streamAll
  )

object OrganisationController {
//  def makeZIO: URIO[OrganisationService & JWTService, OrganisationController] =
  def makeBatchZIO: URIO[Any, OrganisationBatchController] =
    for _ <- ZIO.debug("Creating OrganisationController")
    //      organisationService <- ZIO.service[OrganisationService]
    //      jwtService <- ZIO.service[JWTService]
    yield new OrganisationBatchController(
      // organisationService, jwtService
    )
  def makeStreamZIO: URIO[Any, OrganisationStreamController] =
    for _ <- ZIO.debug("Creating OrganisationController")
    //      organisationService <- ZIO.service[OrganisationService]
    //      jwtService <- ZIO.service[JWTService]
    yield new OrganisationStreamController(
      // organisationService, jwtService
    )

}
