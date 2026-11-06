package dev.cheleb.ziotapir.laminar

import zio.json.*
import zio.stream.*
import zio.test.*

object JsonlParseSpec extends ZIOSpecDefault {

  final case class Place(name: String, rank: Int) derives JsonCodec

  private def bytes(s: String) = ZStream.fromIterable(s.getBytes("UTF-8"))

  def spec = suite("JSONL parse")(
    test("parses one JSON document per line") {
      val jsonl = """{"name":"Paris","rank":1}
                    |{"name":"Zürich","rank":2}
                    |""".stripMargin
      for places <- bytes(jsonl).parse[Place].runCollect
      yield assertTrue(
        places.toList == List(
          Right(Place("Paris", 1)),
          Right(Place("Zürich", 2))
        )
      )
    },
    test("handles lines split across chunks") {
      val jsonl =
        """{"name":"Paris","rank":1}""" + "\n" + """{"name":"Lyon","rank":3}"""
      for places <- bytes(jsonl).rechunk(3).parse[Place].collectRight.runCollect
      yield assertTrue(
        places.toList == List(Place("Paris", 1), Place("Lyon", 3))
      )
    },
    test("reports invalid lines as Left without stopping the stream") {
      val jsonl = "not json\n" + """{"name":"Nice","rank":4}"""
      for places <- bytes(jsonl).parse[Place].runCollect
      yield assertTrue(
        places.size == 2,
        places.head.isLeft,
        places.last == Right(Place("Nice", 4))
      )
    }
  )
}
