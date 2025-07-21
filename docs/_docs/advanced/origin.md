---
layout: main
---


## Origin

The ZIO Laminar Tapir library is designed to integrate ZIO effects with Tapir endpoints, allowing for a seamless interaction between backend services and frontend applications using Laminar. 

3 situations are supported:

* (1) Some origin (default)
The frontend application is served from the same origin as the backend service.
* (2) Different origin
The frontend application is served from a different origin than the backend service.
  * (2.1) Development mode
  In developement mode the frontend application is server by vite, for reloading purposes.
  * (2.2) Production mode
  In production mode the frontend application can also by server a different the backend service.

# Same origin policy

When the frontend application is served from the same origin as the backend service, you can use the endpoints without any additional configuration. The requests will be sent directly to the backend service without any CORS issues.



# Different origin

To enable CORS in your application, you can use the `CORS` middleware. This middleware is included in the `cors` package, so you need to install it first.

When CORS is enabled, the server will respond to preflight requests and add the appropriate headers to the response.

As already mentioned, there are two modes of operation when the frontend application is served from a different origin than the backend service:

* Development mode
In development mode the frontend application is server by vite, for reloading purposes.

By default, Vite will proxy requests to the backend service, on `http://localhost:8080`, so you don't need to configure anything.

If for any reason you need to change the backend service URL, you can do it in the developnent [host page ](../../../examples/client/index.html#L10)

Where the javaScript code is:
```javascript
DEV_API_URL = "http://aserver.com"
```

This URL will be used to proxy requests to the backend service, only in development mode.


* Production mode
In production mode the frontend application can also by server a different the backend service.

All executions methods have an equivalent with an `Uri` as first parameter, which is the base URL for the request. This is where the request will be sent.

```scala sc:nocompile
val httpbin = Uri.unsafeParse("https://httpbin.org")

HttpBinEndpoints.get(()) 
          .emit(httpbin, eventBus)
```

