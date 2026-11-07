package dev.cheleb.ziotapir.server

import zio.*
import zio.json.*
import zio.stream.*
import zio.test.*

object JsonStreamSpec extends ZIOSpecDefault {

  final case class Item(id: Int, name: String) derives JsonCodec

  private val items = List(Item(1, "one"), Item(2, "two"), Item(3, "three"))

  private def asString(stream: ZStream[Any, Throwable, Byte]) =
    stream.runCollect.map(bytes => new String(bytes.toArray, "UTF-8"))

  def spec = suite("JSON stream extensions")(
    suite("ZStream")(
      test("toJsonArrayStream produces a valid JSON array") {
        for json <- asString(ZStream.fromIterable(items).toJsonArrayStream)
        yield assertTrue(json.fromJson[List[Item]] == Right(items))
      },
      test("toJsonArrayStream of an empty stream is an empty array") {
        for json <- asString(ZStream.empty.as(Item(0, "")).toJsonArrayStream)
        yield assertTrue(json.fromJson[List[Item]] == Right(Nil))
      },
      test("toJsonArrayStream(wrapper) wraps the array in an object") {
        for json <- asString(
            ZStream.fromIterable(items).toJsonArrayStream("data")
          )
        yield assertTrue(
          json.fromJson[Map[String, List[Item]]] == Right(Map("data" -> items))
        )
      },
      test("toJsonLinesStream produces one JSON document per line") {
        for json <- asString(ZStream.fromIterable(items).toJsonLinesStream)
        yield assertTrue(
          json.linesIterator.filter(_.nonEmpty).map(_.fromJson[Item]).toList ==
            items.map(Right(_))
        )
      },
      test("non ASCII characters are UTF-8 encoded") {
        val accented = List(Item(1, "Café ✓"), Item(2, "日本 😀"))
        for
          array <- asString(ZStream.fromIterable(accented).toJsonArrayStream)
          lines <- asString(ZStream.fromIterable(accented).toJsonLinesStream)
        yield assertTrue(
          array.fromJson[List[Item]] == Right(accented),
          lines.linesIterator.filter(_.nonEmpty).map(_.fromJson[Item]).toList ==
            accented.map(Right(_))
        )
      }
    ),
    suite("utf8Encode")(
      test("keeps surrogate pairs split across chunks together") {
        val text = "a😀b€"
        for json <- asString(
            ZStream.fromIterable(text).rechunk(1).via(utf8Encode)
          )
        yield assertTrue(json == text)
      }
    ),
    suite("RIO[R, ZStream]")(
      test("toJsonArrayStream maps the inner stream") {
        for
          stream <- ZIO.succeed(ZStream.fromIterable(items)).toJsonArrayStream
          json <- asString(stream)
        yield assertTrue(json.fromJson[List[Item]] == Right(items))
      },
      test("toJsonArrayStream(wrapper) maps the inner stream") {
        for
          stream <- ZIO
            .succeed(ZStream.fromIterable(items))
            .toJsonArrayStream("items")
          json <- asString(stream)
        yield assertTrue(
          json.fromJson[Map[String, List[Item]]] == Right(Map("items" -> items))
        )
      },
      test("toJsonLinesStream maps the inner stream") {
        for
          stream <- ZIO.succeed(ZStream.fromIterable(items)).toJsonLinesStream
          json <- asString(stream)
        yield assertTrue(json.linesIterator.count(_.nonEmpty) == items.size)
      }
    )
  )
}
