# Hercules Gate
Gate is used to transmit events from clients to Apache Kafka.

## Settings
Application is configured through properties file.

### HTTP Server settings
`http.server.host`

`http.server.port`

`http.server.maxContentLength`

`http.server.throttling.capacity`

`http.server.throttling.requestTimeout`

### Kafka Producer settings
See Producer's Config from Apache Kafka documentation. Main settings are presented below.

`producer.bootstrap.servers`

`producer.acks`

`producer.batch.size`

`producer.linger.ms`

`producer.buffer.memory`

`producer.retries`

`producer.retry.backoff.ms`

### Apache Curator settings
`curator.connectString`

`curator.connectionTimeout`

`curator.sessionTimeout`

`curator.retryPolicy.baseSleepTime`

`curator.retryPolicy.maxRetries`

`curator.retryPolicy.maxSleepTime`

### Graphite metrics reporter settings
`metrics.graphite.server.addr`

`metrics.graphite.server.port`

`metrics.graphite.prefix`

`metrics.period`

### Application context settings
`context.instance.id`

`context.environment`

`context.zone`

## Command line
`java $JAVA_OPTS -jar hercules-gate.jar application.properties=file:///path/to/properties/file`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/properties/znode
```

## Quick start
### Initialization
Gate uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by Hercules Init. See Hercules Init for details.

### `application.properties` sample:
```properties
http.server.host=0.0.0.0
http.server.port=6306
http.server.maxContentLength=25165824
http.server.throttling.capacity=1073741824
http.server.throttling.requestTimeout=10000

producer.bootstrap.servers=localhost:9092
producer.acks=all
producer.batch.size=65536
producer.linger.ms=1
producer.buffer.memory=335544320
producer.retries=4
producer.retry.backoff.ms=250

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=3
curator.retryPolicy.maxSleepTime=3000

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default
```
