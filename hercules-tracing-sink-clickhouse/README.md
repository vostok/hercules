# Hercules ClickHouse Tracing Sink
Tracing Sink is used to move tracing spans from Kafka to ClickHouse.

## Settings
Application is configured through properties file.

## Sink settings

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Base settings
`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.batchSize` - size of batch with events, for ClickHouse is recommended to use batches with hundreds of thousands of rows,
 default value: `1000`

`sink.pattern` - pattern of streams are subscribed by consumers 

### Consumer settings
`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

### ClickHouse Sender settings
`sink.sender.pingPeriodMs` - period to update ClickHouse's availability status,
default value: `5000`

`sink.sender.tableName` - table name for tracing spans in ClickHouse,
default value: `tracing_spans`

`sink.sender.clickhouse.url` - JDBC url of ClickHouse DB,
default value: `jdbc:clickhouse://localhost:8123/default`

`sink.sender.clickhouse.properties` - base scope for ClickHouse data source properties, see JDBC driver docs for details

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix is added to metric name

`metrics.period` - the period to send metrics to graphite, default value: `60`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - deployment environment (production, staging and so on)

`context.zone` - id of zone

### Http Server settings
`http.server.ioThreads` - the number of IO threads. Default value: `1`.

`http.server.workerThreads` - the number of worker threads. Default value: `1`.

## Command line
`java $JAVA_OPTS -jar hercules-tracing-sink-clickhouse.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```
### Initialization
Stream of trace spans should be predefined.
Also, table for tracing spans in ClickHouse should be created.

## `application.properties` sample
```properties
application.host=0.0.0.0
application.port=6513

sink.poolSize=1
sink.pollTimeoutMs=5000
sink.batchSize=500000
sink.pattern=traces_*

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.max.partition.fetch.bytes=52428800
sink.consumer.max.poll.interval.ms=240000
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.sender.pingPeriodMs=60000
sink.sender.tableName=tracing_spans

sink.sender.clickhouse.url=jdbc:clickhouse://localhost:8123/default

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default

http.server.ioThreads=1
http.server.workerThreads=1

```