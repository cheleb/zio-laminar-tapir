# WebSockets

ZIO Laminar Tapir also provides support for WebSocket endpoints defined using Tapir. This allows you to create real-time, bidirectional communication channels between the client and server.

## Defining WebSocket Endpoints

You can define a WebSocket endpoint using Tapir's `webSocketBody` directive. For example:

```scala sc:nocompile
val echo: PublicEndpoint[
    Unit,
    Error,
    ZioStreams.Pipe[WebSocketFrame, WebSocketFrame],
    ZioStreams & WebSockets
  ] =
    endpoint.get
      .out(
        webSocketBody[
          WebSocketFrame,
          CodecFormat.TextPlain,
          WebSocketFrame,
          CodecFormat.TextPlain
        ](
          ZioStreams
        )
      )
      .errorOut(jsonBody[Error])
      .description("WebSocket echo endpoint")
```

This defines a WebSocket endpoint that echoes back any text frames it receives.

Then, in the same way as for regular endpoints, you can build a ZIO effect from the endpoint and handle the WebSocket communication. But as WebSockets are bidirectional, this time calling the endpoint will return a `ZIO` effect that produces a `Pipe` for handling incoming and outgoing messages.


```scala sc:nocompile

ws <- WebsocketEndpoint.echo(())

```

You can then use this `Pipe` to send and receive messages over the WebSocket connection.



## Handling WebSocket Communication

To handle WebSocket communication, you can create a `Pipe` that processes incoming messages and produces outgoing messages. For example, to create an echo server that sends back any message it receives:

```scala sc:nocompile

 _ <- ws(
          ZStream
            .fromHubWithShutdown(hub)
                 .tap(msg => result.zEmit(s"Sending: $msg"))
      )
       .runForeach {
        case Text(payload, _, _) =>
          result.zEmit(s"Received: $payload")
        case _ => ZIO.unit
      }
    
```

`ws` is a function that takes a `ZStream` of outgoing messages and returns another `ZStream` of incoming messages. In this example, we create a `ZStream` from a `Hub` to send outgoing messages and process incoming messages by logging them.

This setup allows you to create real-time applications using WebSockets with ZIO Laminar Tapir, leveraging the power of ZIO streams for handling bidirectional communication.

See the [Full Example](@GITHUB_MASTER@/examples/client/src/main/scala/demo/websocket.scala#L40)