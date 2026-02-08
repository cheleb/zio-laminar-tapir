package dev.cheleb.ziotapir

import zio.*
import zio.json.*
import zio.stream.*

/**
  * This package contains utilities for working with Tapir and ZIO, such as converting ZStreams to JSON lines streams.
  */

/**
  * Extension methods for working with ZStreams in the context of Tapir and ZIO.
  */  
extension [R, A: JsonEncoder](z: RIO[R, ZStream[R, Throwable, A]])

  /**
    * Converts a ZStream of JSON-encodable values to a ZStream of bytes representing JSON lines.
    *
    * @return A ZStream of bytes representing JSON lines.
    */
  def toJsonLinesStream: RIO[R, ZStream[R, Throwable, Byte]] =
    z.map(stream =>
      stream >>> JsonEncoder[A].encodeJsonLinesPipeline >>> ZPipeline
        .map[Char, Byte](_.toByte)
    )
