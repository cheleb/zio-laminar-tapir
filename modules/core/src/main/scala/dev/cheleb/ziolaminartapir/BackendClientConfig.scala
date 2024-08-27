package dev.cheleb.ziolaminartapir

import sttp.model.Uri

final case class BackendClientConfig(
    baseUrl: Option[Uri]
)
