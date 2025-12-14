# Design

This document outlines the design principles and architecture of the ZIO Laminar Tapir library.

## Overview

The ZIO Laminar Tapir library is designed to provide a seamless integration between ZIO, Laminar, and Tapir, enabling developers to build reactive web applications with ease.

The library leverages the power of ZIO for effect management, Laminar for reactive UI components, and Tapir for defining HTTP endpoints.

- **ZIO**: A powerful effect system for managing asynchronous and concurrent computations in Scala.
- **Laminar**: A reactive UI library for building web applications in Scala, providing a declarative way to create and manage UI components.
- **Tapir**: A library for defining

## Backend client

Backend client is based on [sttp Fetch backend](https://sttp.softwaremill.com/en/latest/backends/javascript/fetch.html), which provides a simple and efficient way to handle HTTP requests and responses in a reactive manner.

It will be responsible for building ZIO effects from Tapir endpoints.

It will be able to handle:
- Request and response marshalling
- JWT token management when needed
- Streaming responses


## Architecture


![Architecture Diagram](/images/architecture.svg)

This diagram illustrates the architecture of the ZIO Laminar Tapir library, showing how the key components interact with each other.

## Extensions

Scala 3 extensions and enhancements have been made to improve the usability and functionality of the library. These include:

- **Endpoint** extensions for ZIO**: Simplifying the creation of ZIO effects from Tapir endpoints.
- **RIO[BackendClient, O]** extension: Run ZIO effects and emit responses to Laminar event buses.
