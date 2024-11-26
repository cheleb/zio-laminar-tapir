---
layout: main
---

# Streaming

Th `streaming` module provides a way to stream data from a source to a destination. This is useful when you want to process data in chunks, or when you want to avoid loading the entire data into memory.

## Usage

From a streaming endpoint definition:

```scala sc:nocompile

val allStream
      : Endpoint[Unit, Unit, Throwable, Stream[Throwable, Byte], ZioStreams] =
    baseEndpoint
      .tag("Admin")
      .name("organisation stream")
      .get
      .in("api" / "organisation" / "stream")
      .out(
        streamBody(ZioStreams)(
          summon[Schema[Organisation]],
          CodecFormat.TextEventStream()
        )
      )
      .description("Get all organisations")
```

You can then use the `jsonl` method to stream the data:

```scala sc:nocompile
HttpBinEndpoints.allStream
          .on(localhost)(())
          .jsonl[Organisation](organisation => Console.printLine(organisation))
```

The `jsonl` method will parse the incoming data as JSON and emit it to the provided sink.