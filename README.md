# ZIO, Laminar and Tapir

![Sonatype Central](https://maven-badges.sml.io/sonatype-central/dev.cheleb/zio-tapir-laminar_sjs1_3/badge.svg)

zio-laminar-tapir is a Scala library that integrates ZIO, Laminar, and Tapir to build reactive web applications with type-safe HTTP communication.

Live demo: [🚀 Click me for a live demo](https://cheleb.github.io/zio-laminar-tapir/demo/index.html)

## Core Technologies

- **ZIO 2.1.20**: Provides asynchronous and concurrent programming with functional effects
- **Laminar 17.2.1**: Scala.js library for building reactive user interfaces  
- **Tapir 1.11.42**: Defines HTTP API endpoints as immutable data structures
- **STTP 4.0.9**: HTTP client implementation using Fetch API

## Usage

See [docs](https://cheleb.github.io/zio-laminar-tapir/docs/index.html)

For Sttp 3.x use version < 1.x

For Sttp 4.x use version >= 1.x

## Project Structure

The project follows a multi-module architecture:

- **core** (Scala.js): Frontend client functionality with reactive UI integration
- **server** (JVM): Backend server endpoints and route handling
- **shared** (cross-platform): Common types and utilities for both frontend and backend
- **examples/client**: Demo application showcasing library usage
- **docs**: Comprehensive documentation and live demo

## Key Features

### Type-Safe HTTP Client

The `BackendClient` trait provides methods to convert Tapir endpoints into ZIO effects:

```scala
def requestZIO[I, E <: Throwable, O](
  endpoint: Endpoint[Unit, I, E, O, Any]
)(payload: I): Task[O]
```

### JWT Authentication

- Automatic token management with local storage
- Session handling with expiration validation
- Secure endpoint support with token injection

### Reactive UI Integration

Laminar's `Session` trait provides reactive authentication state:

```scala
def apply[A](withoutSession: => A)(
  withSession: UserToken => A
): Signal[A]
```

### Streaming Support

Built-in support for streaming responses with ZIO streams and WebSocket capabilities.

## Usage Pattern

```scala
import dev.cheleb.ziotapir.laminar.*

// 1. Build ZIO effect from Tapir endpoint
val effect = AnEndpoints.get(1)

// 2. Run effect and emit response to event bus
effect.emit(eventBus)
```

## Authentication Flow

1. Tokens stored in localStorage by issuer URL
2. Automatic expiration checking on access
3. Reactive session state updates through Laminar Signals
4. Token injection for secured endpoints

## Development Setup

- Scala 3.7.2 with strict compiler options
- Scala.js for frontend compilation
- SBT build system with custom plugins for documentation
- Live demo available at the project's GitHub Pages

The library simplifies full-stack Scala development by providing a unified, type-safe approach to API communication in reactive web applications.
