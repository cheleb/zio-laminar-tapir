---
layout: main
---

# CORS

To enable CORS in your application, you can use the `CORS` middleware. This middleware is included in the `cors` package, so you need to install it first.

When CORS is enabled, the server will respond to preflight requests and add the appropriate headers to the response.

```scala sc:nocompile
val httpbin = Uri.unsafeParse("https://httpbin.org")

HttpBinEndpoints.get(()) // (1) Note the `on` method
          .emit(httpbin, eventBus)
```

1. The `on` method is used to specify the base URL for the request. This is where the request will be sent.

