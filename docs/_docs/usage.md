# Usage

This document provides an overview of how to use the ZIO Laminar Tapir library in your projects.

## Extensions and Imports

To use the ZIO Laminar Tapir library, you need to import the necessary extensions and classes. The main import is:

```scala
import dev.cheleb.ziotapir.laminar.*
```

This import provides access to the core functionality of the library, including the ability to build ZIO effects from Tapir endpoints and emit responses to Laminar event buses.

### Building ZIO Effects

A first extension turn Tapir endpoints into functions from input to ZIO effects. `I => ZIO[R, E, Out]`

Then, from a Tapir endpoint definition:

```scala sc:nocompile
val getEndpoint: Endpoint[Unit, Int, Throwable, GetResponse, Any] = ???     
```

This defines a Tapir endpoint that takes an `Int` as input and returns a `GetResponse` or an error of type `Throwable`.

Bild a ZIO effect from the endpoint:

```scala sc:nocompile
  val getIO =  getEndpoint(1)
```

Just by calling the endpoint with the required input, which in this case is an `Int`.

```scala sc:nocompile
  val getIO: RIO[BackendClient, GetResponse] = getEndpoint(1)
```

This ZIO effect can be run in the context of a `BackendClient`.

### Running the ZIO Effect

An another extension will provide the necessary environment, `BackendClient` and will run the effect:


## Handling Responses in UI

Now you can run the ZIO effect and emit the response to a Laminar.

### For a `GetResponse`:

```scala sc:nocompile
  val _: Unit = getIO.emit(eventBus)
```

The event bus will receive the response of type `GetResponse`, if error occurs the console will log the error.



### For a `GetResponse` or a `Throwable`:

```scala sc:nocompile
  val _: Unit = getIO.emit(eventBus, errorBus)
```

The `errorBus` will receive any error that occurs while running the ZIO effect.


This is the general pattern for using the ZIO Laminar Tapir library:

1. Define your Tapir endpoints.
2. Build ZIO effects from the endpoints.
3. Emit the responses to the appropriate event buses.


In advanced usages, you can also handle streaming responses, JWT token management, and more, depending on your application's requirements.