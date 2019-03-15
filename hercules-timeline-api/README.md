# Hercules Timeline Api
Timeline Api is used for reading timelines from Apache Kafka.

## Settings
Application is configured through properties file.

### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.nodes` - default value: `127.0.0.1`

`cassandra.port` - default value: `9042`

`cassandra.keyspace` - default value: `hercules`

`cassandra.readTimeoutMs` - default value: `12000`

### Apache Curator settings
See Apache Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### HTTP Server settings
`http.server.host` - server host, default value: `"0.0.0.0"`

`http.server.port` - server port

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-timeline-api.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Timeline Api uses Timeline's metadata, Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties
cassandra.nodes=localhost:2181,localhost:2182,localhost:2183
cassandra.port=9042
cassandra.keyspace=hercules
cassandra.readTimeoutMs=12000

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000


metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

http.server.host=0.0.0.0
http.server.port=6306

context.instance.id=1
context.environment=dev
context.zone=default
```
