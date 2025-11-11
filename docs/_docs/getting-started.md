---
title: Getting Started
---
# Getting Started

## Installation


```sbt
// ScalaJs
libraryDependencies += "dev.cheleb"    %%% "zio-tapir-laminar"  % "{{ projectVersion}}"
```

Last version is `{{ projectVersion }} it depends on sttp 4.x.

For sttp 3.x use version < 1.x


## Sample



```scala sc:nocompile
import zio.*
import zio.json.*
import sttp.tapir.*

import dev.cheleb.ziotapir.*                        // (1)
import dev.cheleb.ziotapir.laminar.*                // (1)

// From a classical tapir:
// 
// With a response type `GetResponse`
case class GetResponse(
     args: Map[String, String],
     headers: Map[String, String]
) derives JsonCodec

// Create an endpoint that handles a GET request
val get = endpoint.get                             // (2)
     .in("get")
     .out(jsonBody[GetResponse])
     .errorOut(statusCode and plainBody[String])
     .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

// Create an event bus for the response type
val eventBus = EventBus[GetResponse]()             // (3)

// Use the endpoint as as  ZIO effect
get(())                                            // (4) 
 .emit(eventBus)                                   // (5)
```



* (1) Import the library, which provides extensions method on `Endpoint` instances.
  * Import the necessary classes and implicits from the library.
  * Impot the `HttpError` class for error handling.
* (2) Create an `Endpoint` instance using the Tapir DSL, defining the input, output, and error types.
  * Notice that the output is a JSON body of type `GetResponse`, and the error output is a combination of a status code and a plain body.
  * Error muste be encoded and decoded using `HttpError.encode` and `HttpError.decode`.

* (3) Create an `EventBus` for the response `GetResponse`.
* (4) Use the endpoint as a function from `Input => ZIO[Backend, Throwable, GetResponse]`.
* (5) Call the `emit` method on the endpoint, passing the `EventBus` instances.