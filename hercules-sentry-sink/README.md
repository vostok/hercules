# Hercules Sentry Sink
Sentry Sink is used to move Log Event with exceptions from Kafka to Sentry.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Sink settings
`sink.pattern` - pattern of streams are subscribed by consumers 

`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

`sink.sender.sentry.url` - URL of Sentry

`sink.sender.sentry.token` - token of Sentry user. It is used for authentication on Sentry

`sink.sender.sentry.level` - log level. Logs with this level and higher levels could be sent to Sentry. Default value: `WARNING`

`sink.sender.sentry.retryLimit` - the number of attempts to send event with retryable errors, default value: `3`

### Rate Limiting settings 

`sink.sender.throttling.rate.limit` - max event count per time window. Default value: `1000`.

`sink.sender.throttling.rate.timeWindowMs` - time window in millis to apply event limit. Default value:  `60000` ms.

### Timeout settings
`sink.sender.connectionTimeoutMs` - timeout of connection for sending an event to Sentry. Default value: `1000` ms.

`sink.sender.readTimeoutMs` - timeout of reading when an event is sent to Sentry. Default value: `5000` ms.

### Application context settings
`context.environment` - id of environment

`context.zone` - id of zone

`context.instance.id` - id of instance

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Http Server settings
`http.server.ioThreads` - the number of IO threads. Default value: `1`.

`http.server.workerThreads` - the number of worker threads. Default value: `1`.

## Command line
`java $JAVA_OPTS -jar hercules-sentry-sink.jar application.properties=file://path/to/properties/file`

## Quick start
### Initialization

Stream with log events should be predefined.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6511

sink.sender.sentry.url=https://sentry.io
sink.sender.sentry.token=1234567890768132cde645f1ba1bcd4ef67ab78cd9ef89801a45be5747c68f87
sink.sender.sentry.level=warning

sink.sender.throttling.rate.limit=5000
sink.sender.throttling.rate.timeWindowMs=300000

sink.sender.connectionTimeoutMs=10000
sink.sender.readTimeoutMs=25000

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.pattern=mystream

context.environment=dev
context.zone=default
context.instance.id=single

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=myprefix
metrics.period=60

http.server.ioThreads=1
http.server.workerThreads=1
```
