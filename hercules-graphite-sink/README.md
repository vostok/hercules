# Hercules Graphite Sink
Graphite Sink is used to move metric events without any planned aggregation from Kafka to Graphite.

## Settings
Application is configured through properties file.

### Sink settings
`sink.poolSize` - number of threads the are reading from Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when reading from Kafka, default value: `6000`

`sink.batchSize` - preferred size of event batches, default value: `1000`

`sink.pattern` - pattern of topic names to read from

`sink.consumer.bootstrap.servers` - list of Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - timeout after which Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

`sink.sender.graphite.host` - Graphite host address

`sink.sender.graphite.port` - Graphite port

`sink.sender.pingPeriodMs` - Graphite server ping period in case of unavailability, default value: `5000`

`sink.sender.retryLimit` - maximum attempts count when sending metrics to Graphite, default value: `3`

`sink.sender.diagnosticLogWritePeriodMs` - timeout for log count sent metrics to Graphite, default value: `60000`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix is added to metric name

`metrics.period` - the period to send metrics to graphite, default value: `60`

### HTTP Server settings
`http.server.host` - server host, default value: `"0.0.0.0"`

`http.server.port` - server port

### Application context settings
`context.instance.id` - id of instance

`context.environment` - deployment environment (production, staging and so on)

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-graphite-sink.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Streams with log events should be predefined.

### `application.properties` sample:
```properties
sink.poolSize=4
sink.pollTimeoutMs=5000
sink.batchSize=10000
sink.pattern=metrics_*

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.max.partition.fetch.bytes=8388608
sink.consumer.max.poll.interval.ms=370000
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.sender.graphite.host=graphite.ru
sink.sender.graphite.port=2003
sink.sender.retryLimit=3
sink.sender.pingPeriodMs=30000
sink.sender.diagnosticLogWritePeriodMs=60000

metrics.graphite.server.addr=graphite.ru
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

http.server.host=0.0.0.0
http.server.port=6501 

context.instance.id=1
context.environment=dev
context.zone=default
```
