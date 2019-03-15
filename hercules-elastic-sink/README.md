# Hercules Elastic Sink
Elastic Sink is used to move Log Events from Kafka to Elasticsearch.

## Settings
Application is configured through properties file.

### Sink settings
`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

`sink.senderTimeoutMs` - time quota to process Log Events by elastic, default value: `2000`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.batchSize` - size of batch with Log Events, default value: `1000`

`sink.pattern` - pattern of streams are subscribed by consumers 

`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.sender.elastic.hosts` - list of elastic hosts

`sink.sender.elastic.retryTimeoutMs` - backoff timeout to retry send to elastic, default value: `30000`

`sink.sender.elastic.connectionTimeoutMs` - connection timeout of elastic client, default value: `1000`

`sink.sender.elastic.connectionRequestTimeoutMs` - timeout for request connection of elastic client, default value: `500`

`sink.sender.elastic.socketTimeoutMs` - timeout for response from elastic, default value: `30000`

`sink.sender.pingPeriodMs` - elastic server ping period, default value: `5000`

`sink.sender.retryOnUnknownErrors` - should retry request to elastic in case of unknown errors, default value: `false`

`sink.sender.retryLimit` - count of trying send batch with retryable errors, default value: `3`

`sink.sender.elastic.mergePropertiesTagToRoot` - flag for moving the contents of the properties container to the root of the object, default value: `false`

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
`java $JAVA_OPTS -jar hercules-elastic-sink.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Streams with log events should be predefined.

### `application.properties` sample:
```properties
sink.poolSize=3
sink.senderTimeoutMs=120000
sink.pollTimeoutMs=5000
sink.batchSize=10000
sink.pattern=abc_*

sink.consumer.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
sink.consumer.max.partition.fetch.bytes=8388608
sink.consumer.max.partition.fetch.bytes=52428800

sink.consumer.max.poll.interval.ms=370000

sink.sender.elastic.hosts=localhost:9201

sink.sender.elastic.retryTimeoutMs=120000
sink.sender.elastic.connectionTimeoutMs=1000
sink.sender.elastic.connectionRequestTimeoutMs=500
sink.sender.elastic.socketTimeoutMs=120000
sink.sender.pingPeriodMs=60000
sink.sender.retryOnUnknownErrors=true
sink.sender.retryLimit=2
sink.sender.elastic.mergePropertiesTagToRoot=true

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
