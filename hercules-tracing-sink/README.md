# Hercules Tracing Sink
Tracing Sink is used to move traces from Kafka to Apache Cassandra.

## Settings
Application is configured through properties file.

## Sink settings

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Base settings
`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.batchSize` - size of batch with events, default value: `1000`

`sink.pattern` - pattern of streams are subscribed by consumers 

### Consumer settings
`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

### Cassandra Sender settings
`sink.sender.pingPeriodMs` - period to update Cassandra's availability status, default value: `5000`

`sink.sender.sendTimeoutMs` - timeout for sending requests to Cassandra, default value: `60000`

`sink.sender.batchSize` - limit for statements in a single batch, default value: `10`

`sink.sender.tableName` - table name for trace spans in Cassandra, default value: `tracing_spans`

`sink.sender.cassandra.dataCenter` - local Cassandra DC, default value: `datacenter1`

`sink.sender.cassandra.nodes` - nodes of Cassandra in form `<host>[:port][,<host>[:port],...]`, default value: `127.0.0.1`,
 also, default port value is `9042`

`sink.sender.cassandra.keyspace` - keyspace in Cassandra, default value: `hercules`

`sink.sender.cassandra.requestTimeoutMs` - request to Cassandra timeout, default value: `12000`

`sink.sender.cassandra.connectionsPerHostLocal` - connections per local Cassandra node (see Cassandra docs for details), default value: `4`

`sink.sender.cassandra.connectionsPerHostRemote` - connections per remote Cassandra node (see Cassandra docs for details), default value: `2`

`sink.sender.cassandra.maxRequestsPerConnection` - max requests per connection, default value: `1024`

`sink.sender.cassandra.consistencyLevel` - consistency level (see Cassandra docs for details), default value: `QUORUM`

`sink.sender.cassandra.auth.enable` - if Cassandra requires authentication then set this property value to `true`
and specify credential in the respective properties, default value: `false`

`sink.sender.cassandra.auth.provider.username` - username which is needed for Cassandra authentication.
*Required* if `cassandra.auth.enable` is set to `true`, otherwise value is *ignored*.

`sink.sender.cassandra.auth.provider.password` - password which is needed for Cassandra authentication.
*Required* if `cassandra.auth.enable` is set to `true`, otherwise value is *ignored*.

`sink.sender.cassandra.auth.provider.class` - name of the class which is needed for Cassandra authentication.
Only classes that implements `com.datastax.driver.core.AuthProvider` should be specified, default value: `PlainTextAuthProvider`

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
`java $JAVA_OPTS -jar hercules-tracing-sink.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```
### Initialization
Stream of trace spans should be predefined.
Also, table for trace spans in Cassandra should be created.

See [hercules-init](../hercules-init/README.md) for details.

## `application.properties` sample
```properties
application.host=0.0.0.0
application.port=6510

sink.poolSize=1
sink.pollTimeoutMs=5000
sink.batchSize=10000
sink.pattern=traces_*

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.max.partition.fetch.bytes=52428800
sink.consumer.max.poll.interval.ms=240000
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.sender.pingPeriodMs=60000
sink.sender.sendTimeoutMs=60000
sink.sender.batchSize=10
sink.sender.tableName=tracing_spans

sink.sender.cassandra.dataCenter=datacenter1
sink.sender.cassandra.nodes=localhost:9042,localhost:9043,localhost:9044
sink.sender.cassandra.keyspace=hercules_traces
sink.sender.cassandra.requestTimeoutMs=12000
sink.sender.cassandra.connectionsPerHostLocal=4
sink.sender.cassandra.connectionsPerHostRemote=2
sink.sender.cassandra.maxRequestsPerConnection=1024
sink.sender.cassandra.consistencyLevel=QUORUM

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