package dev.cheleb.ziotapir.client

import zio.*
import dev.cheleb.ziotapir.BackendClient

extension [A](zio: RIO[BackendClient, A])
  def send(using backend: BackendClient): Task[A] =
    zio.provide(ZLayer.succeed(backend))
