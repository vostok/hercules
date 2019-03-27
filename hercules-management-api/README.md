# Hercules Management Api
Management Api is used for working with streams, timelines, rules and blacklist in Apache Kafka.
Provide next opportunities:
* Streams:
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for create stream;
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for delete stream;
  * Show all streams;
  * Get info about stream.
  
* Timelines:
  * Gives the command to [Timeline Manager](../hercules-timeline-manager/README.md) for creat timeline;
  * Gives the command to [Timeline Manager](../hercules-timeline-manager/README.md) for delete timeline;
  * Show all timelines;
  * Get info about timeline.

* Rules:
  * Create rule;
  * Show all rules.
  
* Blacklist:
  * Add apiKey to blacklist;
  * Remove apiKey from blacklist;
  * Show all records in blacklist.
## Settings
Application is configured through properties file.

### Apache Curator settings
See Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### Apache Kafka settings
See Apache Kafka Config from Apache Kafka documentation. Main settings are presented below.

`kafka.bootstrap.servers`

`kafka.acks`

`kafka.retries`

`kafka.batch.size`

`kafka.linger.ms`

`kafka.buffer.memory`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Http Server settings
`http.server.host` - server host, default value: `0.0.0.0`

`http.server.port` - server port, default value: `6309`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-management-api.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Management Api uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000

kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=0
kafka.batch.size=16384
kafka.linger.ms=1
kafka.buffer.memory=33554432

keys=123,456

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

http.server.host=0.0.0.0
http.server.port=6309

context.instance.id=1
context.environment=dev
context.zone=default
```
