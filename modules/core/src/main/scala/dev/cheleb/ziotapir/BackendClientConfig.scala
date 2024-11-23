package dev.cheleb.ziotapir

import sttp.model.Uri

/** Configuration for the backend client.
  *
  * @param baseUrl
  */
final case class BackendClientConfig(
    baseUrl: Uri
):
  val baseUrlAsOption: Option[Uri] = Some(baseUrl)
