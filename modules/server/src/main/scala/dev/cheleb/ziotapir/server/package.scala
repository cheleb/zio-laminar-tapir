package dev.cheleb.ziotapir.server

import zio.*
import zio.json.*
import zio.stream.*

import java.nio.charset.StandardCharsets.UTF_8

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

/** UTF-8 encodes a stream of chars.
  *
  * A surrogate pair split across two chunks is kept until the next chunk, so
  * that it is encoded as a single code point.
  */
private[server] val utf8Encode: ZPipeline[Any, Nothing, Char, Byte] =
  ZPipeline.chunks[Char] >>>
    ZPipeline.mapAccum[Chunk[Char], Option[Char], Chunk[Byte]](None) {
      (pending, chunk) =>
        val chars = pending.fold(chunk)(_ +: chunk)
        def encode(cs: Chunk[Char]) =
          Chunk.fromArray(new String(cs.toArray).getBytes(UTF_8))
        if chars.lastOption.exists(_.isHighSurrogate) then
          (chars.lastOption, encode(chars.dropRight(1)))
        else (None, encode(chars))
    } >>> ZPipeline.flattenChunks

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
    stream >>> JsonEncoder[A].encodeJsonArrayPipeline >>> utf8Encode

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
      s"{ \"$wrapper\": ".getBytes(UTF_8)
    ) ++ toJsonArrayStream ++ ZStream('}'.toByte)

  /** Converts a ZStream of JSON-encodable values to a ZStream of bytes
    * representing JSON lines.
    *
    * @return
    *   A ZStream of bytes representing JSON lines.
    */
  def toJsonLinesStream: ZStream[R, Throwable, Byte] =
    stream >>> JsonEncoder[A].encodeJsonLinesPipeline >>> utf8Encode

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
    z.map(_.toJsonLinesStream)
