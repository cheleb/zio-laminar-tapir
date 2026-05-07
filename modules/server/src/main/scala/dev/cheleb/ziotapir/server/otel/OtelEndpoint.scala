package dev.cheleb.ziotapir.server.otel

trait OtelEndpoint {
  def otelEndpoint: String =
    sys.env.getOrElse(
      "OTEL_EXPORTER_OTLP_ENDPOINT",
      "http://localhost:4317"
    )
}
