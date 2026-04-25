package dev.cheleb.ziotapir

import zio.*
import zio.json.*
import zio.stream.*

/** This package contains utilities for working with Tapir and ZIO, such as
  * converting ZStreams to JSON streams.
  *
  *   - `toJsonArrayStream`: Converts a ZStream of JSON-encodable values to a
  *     ZStream of bytes representing a JSON array.
  *   - `toJsonArrayStream(wrapper: String)`: Converts a ZStream of JSON
  *     encodable values to a ZStream of bytes representing a JSON array wrapped
  *     in an object with the given wrapper name.
  *   - `toJsonLinesStream`: Converts a ZStream of JSON-encodable values to a
  *
  * ZStream of bytes representing JSON lines. These extension methods can be
  * used to easily convert ZStreams of data into formats suitable for HTTP
  * responses in Tapir endpoints.
  */

/** Extension methods for working with ZStreams in the context of Tapir.
  */
extension [R, A: JsonEncoder](stream: ZStream[R, Throwable, A])
  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON array.
    *
    * @return
    *   A ZStream of bytes representing JSON array.
    */
  def toJsonArrayStream: ZStream[R, Throwable, Byte] =
    stream >>> JsonEncoder[
      A
    ].encodeJsonArrayPipeline >>> ZPipeline
      .map[Char, Byte](_.toByte)

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON array wrapped in an object with the given wrapper name.
    *
    * Example output for wrapper "data":
    * ```
    *   {
    *     "data": [ ... ]
    *   }
    * ```
    *
    * @param wrapper
    *   The name of the wrapper object.
    * @return
    *   A ZStream of bytes representing JSON array wrapped in an object.
    */
  def toJsonArrayStream(
      wrapper: String
  ): ZStream[R, Throwable, Byte] =
    ZStream.fromIterable(
      s"{ \"$wrapper\": ".getBytes()
    ) ++ toJsonArrayStream ++ ZStream('}')

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON lines.
    *
    * @return
    *   A ZStream of bytes representing JSON lines.
    */
  def toJsonLinesStream: ZStream[R, Throwable, Byte] =
    stream >>> JsonEncoder[A].encodeJsonLinesPipeline >>> ZPipeline
      .map[Char, Byte](_.toByte)

/** Extension methods for working with ZIO producing ZStreams in the context of
  * Tapir.
  */
extension [R, A: JsonEncoder](z: RIO[R, ZStream[R, Throwable, A]])

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON array.
    *
    * @return
    *   A ZStream of bytes representing JSON array.
    */
  def toJsonArrayStream: RIO[R, ZStream[R, Throwable, Byte]] =
    z.map(_.toJsonArrayStream)

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON array wrapped in an object with the given wrapper name.
    *
    * Example output for wrapper "data":
    * ```
    *   {
    *     "data": [ ... ]
    *   }
    * ```
    *
    * @param wrapper
    *   The name of the wrapper object.
    * @return
    *   A ZStream of bytes representing JSON array wrapped in an object.
    */
  def toJsonArrayStream(wrapper: String): RIO[R, ZStream[R, Throwable, Byte]] =
    z.map(_.toJsonArrayStream(wrapper))

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON lines.
    *
    * @return
    *   A ZStream of bytes representing JSON lines.
    */
  def toJsonLinesStream: RIO[R, ZStream[R, Throwable, Byte]] =
    z.map(stream =>
      stream >>> JsonEncoder[A].encodeJsonLinesPipeline >>> ZPipeline
        .map[Char, Byte](_.toByte)
    )
