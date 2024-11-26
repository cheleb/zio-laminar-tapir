---
title: Getting Started
---
# Getting Started

## Installation

```sbt
// ScalaJs
libraryDependencies += "dev.cheleb"    %%% "zio-tapir-laminar"  % "{{ projectVersion}}"
```


## Sample

From a classical tapir endpoint definition:



```scala sc:nocompile
import zio.*
import zio.json.*
import sttp.tapir.*

case class GetResponse(args: Map[String, String]) derives JsonCodec

trait BaseEndpoint {
  val baseEndpoint: Endpoint[Unit, Unit, Throwable, Unit, Any] = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val baseSecuredEndpoint: Endpoint[String, Unit, Throwable, Unit, Any] =
    baseEndpoint
      .securityIn(auth.bearer[String]())
}

```

In your Laminar app:

```scala sc:nocompile

import dev.cheleb.ziotapir.laminar.*                // (1)

val eventBus = new EventBus[GetResponse]()          // (2)
val errorBus = new EventBus[Throwable]()            // (3)

// ...

button(
    "runJs",
    onClick --> (_ => HttpBinEndpoints.get(())      // (4)
                        .runJs(eventBus, errorBus)  // (5)
  )
)
```

1. Import the library, which provides extensions method on `Endpoint` instances (like in first approximation `runJs`).
2. Create an `EventBus` for the response type.
3. Create an `EventBus` for the error type.
4. Use the endpoint as a function from `Input => ZIO`.
5. Call the `runJs` method on the endpoint, passing the `EventBus` instances.