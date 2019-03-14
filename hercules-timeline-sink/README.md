# Hercules Timeline Sink
Timeline Sink is used to move timelines from Kafka to Apache Cassandra.

## Settings
Application is configured through properties file.

### Apache Cassandra settings 
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.nodes`

`cassandra.port`

`cassandra.keyspace`

`cassandra.readTimeoutMs`

### Apache Curator settings
See Apache Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString`

`curator.connectionTimeout`

`curator.sessionTimeout`

`curator.retryPolicy.baseSleepTime`

`curator.retryPolicy.maxRetries`

`curator.retryPolicy.maxSleepTime`


### Stream settings
`streams.bootstrap.servers` - list of host/port pairs to use for establishing the initial connection to the Kafka cluster

### Timeline settings
`sink.timeline` - name of timeline

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### HTTP Server settings
`http.server.host` - server host

`http.server.port` - server port

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-timeline-sink.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Timeline Sink uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties
cassandra.nodes=localhost
cassandra.port=9042
cassandra.keyspace=hercules
cassandra.readTimeoutMs=12000

curator.connectString=localhost:2181,localhost:2182,localhost:2183
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=3
curator.retryPolicy.maxSleepTime=3000


streams.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094

sink.timeline=test_timeline

metrics.graphite.server.addr=graphite.ru
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

http.server.host=0.0.0.0
http.server.port=6509

context.instance.id=1
context.environment=dev
context.zone=default
```