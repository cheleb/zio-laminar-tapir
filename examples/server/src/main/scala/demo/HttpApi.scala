package demo

import zio.*

import dev.cheleb.tapir.*
import dev.cheleb.tapir.ztapir.*

import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.zio.ZioStreams

//https://tapir.softwaremill.com/en/latest/server/logic.html
//type Deps = UserService & JWTService & OrganisationService & MeshService
type Deps = Any
object HttpApi {

  val makeBatchControllers: ZIO[Any, Nothing, List[BatchController[Task]]] =
    for {
      _ <- ZIO.debug(
        "*******************\nGathering endpoints\n*****************"
      )
      organisationController <- OrganisationController.makeBatchZIO
    } yield List(
      organisationController
    )

  val makeStreamControllers
      : ZIO[Any, Nothing, List[StreamController[ZioStreams, Task]]] =
    for {
      _ <- ZIO.debug(
        "*******************\nGathering endpoints\n*****************"
      )
      organisationController <- OrganisationController.makeStreamZIO
    } yield List(
      organisationController
    )

  private def gatherAllRoutes
      : URIO[Deps, List[ServerEndpoint[ZioStreams, Task]]] =
    for {
      batchEndpoints <- makeBatchControllers.batchRoutes

      streamEndpoints <- makeStreamControllers.streamRoutes
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
