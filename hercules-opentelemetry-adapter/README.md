# Hercules OpenTelemetry Adapter
OpenTelemetry Adapter implements OpenTelemetry API for traces.    
Receives traces in the OpenTelemetry proto format and sends as Hercules events to the [Gate](../hercules-gate/README.md).

See OpenTelemetry docs for details:
- [Documentation](https://opentelemetry.io/docs/).
- [OpenTelemetry proto](https://github.com/open-telemetry/opentelemetry-proto).

## Settings
Application is configured through properties file.

### gRPC server settings
`grpc.server.port` - gRPC server port, default value: `4317`

### Services settings
`trace.service.stream` - trace stream, required

### Gate Client settings
`gate.client.apiKey` - Hercules API key for streams, required

`gate.client.urls` - Gate topology, required

### Main Application settings
`application.host` - HTTP-server host, default value: `0.0.0.0`

`application.port` - HTTP-server port, default value: `8080`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix is added to metric name

`metrics.period` - the period to send metrics to graphite, default value: `60`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - deployment environment (production, staging and so on)

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-opentelemetry-adapter.jar application.properties=file://path/to/file/application.properties`

## Quick start
### Initialization
Stream of trace spans should be predefined.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6403

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default

grpc.server.port=4317

trace.service.stream=traces_cloud

gate.client.apiKey=1234
gate.client.urls=http://localhost:6306
```
