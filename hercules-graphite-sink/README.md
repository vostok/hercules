# Hercules Graphite Sink
Graphite Sink is used to move metric events without any planned aggregation from Kafka to Graphite.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Sink settings
`sink.poolSize` - number of threads the are reading from Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when reading from Kafka, default value: `6000`

`sink.batchSize` - preferred size of event batches, default value: `1000`

`sink.pattern` - pattern of topic names to read from

`sink.consumer.bootstrap.servers` - list of Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - timeout after which Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

`sink.sender.pingPeriodMs` - ping period in case of unavailability, default value: `5000`

`sink.sender.retryLimit` - maximum attempts count when sending metrics to Graphite, default value: `3`

`sink.sender.graphite.tags.enable` - sending metrics with tags, default value: `false`

#### Graphite connector settings
`sink.sender.graphite.connector.local.connection.limit.per.endpoint` - maximum connections per local endpoint, default value: `3`

`sink.sender.graphite.connector.local.frozen.time.ms` - time to freeze local endpoint in milliseconds, default value : `30 000`

`sink.sender.graphite.connector.local.endpoints` - list of local Graphite endpoints in form `host:port`, required

`sink.sender.graphite.connector.remote.connection.limit.per.endpoint` - maximum connections per remote endpoint, default value: `3`

`sink.sender.graphite.connector.remote.frozen.time.ms` - time to freeze remote endpoint in milliseconds, default value : `30 000`

`sink.sender.graphite.connector.remote.endpoints` - list of remote Graphite endpoints in form `host:port`, optional

### Filters settings

`sink.filter.list` - list of filter classes. Set value: `ru.kontur.vostok.hercules.graphite.sink.filter.MetricEventFilter,ru.kontur.vostok.hercules.graphite.sink.filter.MetricAclEventFilter`

`sink.filter.1.acl.path` - path to access control list. ACL file contains rules (each on a new line).
Rules are written as `<statement> <pattern>`, where `<statement>` - action applied to the metric event (DENY or PERMIT),
`<pattern>` - pattern for matching metric name. 

Example: 
```
DENY value
PERMIT value.*
```

`sink.filter.1.acl.defaultStatement` - default statement, default value: `DENY`

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
application.host=0.0.0.0
application.port=6512

sink.poolSize=4
sink.pollTimeoutMs=5000
sink.batchSize=10000
sink.pattern=metrics_*

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.max.partition.fetch.bytes=8388608
sink.consumer.max.poll.interval.ms=370000
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.sender.retryLimit=3
sink.sender.pingPeriodMs=30000
sink.sender.graphite.tags.enable=false
sink.sender.graphite.connector.local.connection.limit.per.endpoint=3
sink.sender.graphite.connector.local.frozen.time.ms=30000
sink.sender.graphite.connector.local.endpoints=localhost:2003
sink.sender.graphite.connector.remote.endpoints=

sink.filter.list=ru.kontur.vostok.hercules.graphite.sink.filter.MetricEventFilter,ru.kontur.vostok.hercules.graphite.sink.filter.MetricAclEventFilter
sink.filter.1.acl.path=file://metrics.acl
sink.filter.1.acl.defaultStatement=DENY

metrics.graphite.server.addr=graphite.ru
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default

http.server.ioThreads=1
http.server.workerThreads=1
```
