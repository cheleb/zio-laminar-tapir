# Secured Endpoints

The good news is that the ZIO Laminar Tapir library provides a way to handle secured endpoints using JWT tokens. This allows you to protect your endpoints and ensure that only authorized users can access them.

Without the need to write any additional code.

From Tapir point of view, you can define your endpoints as secured by adding a security scheme to the endpoint definition.

```scala sc:nocompile

val securedEndpoint: Endpoint[String, Int, Throwable, GetResponse, Any] = 
  endpoint.get
    .securityIn(auth.bearer[String]("token"))
    .in("secured")
    .in(query[Int]("id"))
    .out(jsonBody[GetResponse])
    .errorOut(jsonBody[Throwable])
```



## JWT Token Management

The prsence of the `auth.bearer` security scheme in the endpoint definition indicates that the endpoint is secured with a JWT token. The token is expected to be passed in the `Authorization` header of the request.

This will activate an extension thay will need a `JWT` token to be passed in the request header.

This token can be generated and managed using the `JWT` library, which provides a way to create and verify JWT tokens.

Then in the context of the frontend application, you will need to provide a:

```scala sc:nocompile
given Session = MySession[MyToken]
```

And that's it, the ZIO Laminar Tapir library will take care of the rest, ensuring that the JWT token is included in the request headers when calling the secured endpoint.