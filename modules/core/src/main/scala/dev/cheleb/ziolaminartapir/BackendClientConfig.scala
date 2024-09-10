package dev.cheleb.ziolaminartapir

import sttp.model.Uri

final case class BackendClientConfig(
    baseUrl: Uri
):
  val baseUrlAsOption: Option[Uri] = Some(baseUrl)
