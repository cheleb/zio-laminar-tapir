package demo

import zio.*

import dev.cheleb.ftapir.*
//import dev.cheleb.ziotapir.*

import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.zio.ZioStreams

//https://tapir.softwaremill.com/en/latest/server/logic.html
//type Deps = UserService & JWTService & OrganisationService & MeshService
type Deps = Any
object HttpApi extends Routes[ZioStreams, Task] {

  val makeBatchControllers: ZIO[Any, Nothing, List[BaseController[Any, Task]]] =
    for {
      _ <- ZIO.debug(
        "*******************\nGathering endpoints\n*****************"
      )
//      organisationController <- OrganisationController.makeZIO
    } yield List(
      //    organisationController
    )

  val makeStreamControllers
      : ZIO[Any, Nothing, List[BaseController[ZioStreams, Task]]] =
    for {
      _ <- ZIO.debug(
        "*******************\nGathering endpoints\n*****************"
      )
      organisationController <- OrganisationController.makeZIO
    } yield List(
      organisationController
    )

  private def endpointsZIO(
      ctrs: URIO[Deps, List[BaseController[Any, Task]]]
  ) = ctrs.map(gatherBatchRoutes(_.routes))

  private def streamEndpointsZIO(
      ctrs: URIO[Deps, List[BaseController[ZioStreams, Task]]]
  ) =
    ctrs.map(gatherStreamRoutes(_.streamRoutes))

  private def gatherAllRoutes
      : URIO[Deps, List[ServerEndpoint[ZioStreams, Task]]] =
    for {
      batchEndpoints <- endpointsZIO(makeBatchControllers)
      streamEndpoints <- streamEndpointsZIO(makeStreamControllers)
    } yield batchEndpoints ++ streamEndpoints

  /** This is critical, to not provide the Postgres layer too early, it would be
    * closed too early in the app lifecycle.
    */
  def endpoints =
    gatherAllRoutes // .provideSome[Postgres[SnakeCase]](
    // Service layers
    // UserServiceLive.layer,
    // OrganisationServiceLive.layer,
    // MeshServiceLive.layer,
    // JWTServiceLive.configuredLayer,
    // // Repository layers
    // UserRepositoryLive.layer,
    // OrganisationRepositoryLive.layer,
    // MeshRepositoryLive.layer

    // , ZLayer.Debug.mermaid
    // )
}
