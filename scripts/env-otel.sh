#!/usr/bin/env bash


# Get the current directory name to use as domain identifier
# More suitable with orbstack dns setup
DOMAIN=$(basename `pwd`)


while getopts "o" opt; do
case $opt in
  o)
    echo "OpenTelemetry logging enabled."
    export OTEL_SERVICE_NAME="earn-simulator"
    # Otel
    export OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED="true"
    export OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
    export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317"
    export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT="http://localhost:4317"
    export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT="http://localhost:4317"
    export OTEL_RESOURCE_PROVIDERS_AWS_ENABLED="true"
    ;;

  *)
    echo "Invalid option: -${opt} -o to enable OpenTelemetry logging"
    exit 1
    ;;
esac
done
shift $((OPTIND-1))

"$@"
