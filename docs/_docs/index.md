---
layout: main
---

## ZIO * Laminar * Tapir


zio-Laminar-tapir is a library that leverages the power of [ZIO](https://zio.dev/), [Laminar](https://laminar.dev/) and [Tapir](https://tapir.softwaremill.com/en/latest/) to build web applications in Scala.

Under the hood, it uses [Fetch client](https://sttp.softwaremill.com/en/latest/backends/javascript/fetch.html) to handle HTTP requests and responses, allowing you to build reactive web applications with ease.


It provides a simple and powerful way, from a [Tapir endpoints](https://tapir.softwaremill.com/en/latest/endpoint/basics.html#basics) definition, to handle requests and responses, and manage client-side rendering.

```scala sc:nocompile
// A single import
import dev.cheleb.ziotapir.laminar.*

AnEndpoints
            .get(1)         // (1) Build an ZIO effect from a tapir endpoint
            .emit(eventBus) // (2) Run the effect and emit the response to an event bus
```

In two steps:

1. Build a ZIO effect from a Tapir endpoint definition
2. Run the effect and emit the response to an event bus

It will handle:

* request and response marshalling
* jwt token when needed
* client management

<span onclick='window.open("../demo/index.html", "_blank")'>🚀 Click me for a live demo</span>



## Dependencies

This project is built on top of:

* [Scala 3](https://docs.scala-lang.org/scala3/) for compile-time metaprogramming and [ScalaJS](https://www.scala-js.org/) for client-side rendering.
* [ZIO](https://zio.dev/) for functional effects and concurrency
* [Laminar](https://laminar.dev)
* [Tapir](https://tapir.softwaremill.com/en/latest/) for endpoint definition



## Credits

Incredible thanks to incredible peoples who made this possible, authors and contributors of:

* librairies this project depends on !
  * [ZIO](https://zio.dev/)
  * [Laminar](https://laminar.dev/)
  * [Tapir](https://tapir.softwaremill.com/en/latest/)
  * [Scala 3](https://docs.scala-lang.org/scala3/)
* Strongly inspired by non less incredible <3 Daniel Ciocîrlan's blogs, videos and courses <3 from Rock the JVM [ZIO Laminar](https://rockthejvm.com/p/zio-rite-of-passage) course

