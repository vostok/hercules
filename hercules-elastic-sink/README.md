# Hercules Elastic Sink
Elastic Sink is used to move Log Events from Kafka to Elasticsearch.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Sink settings
#### Common settings
`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.batchSize` - size of batch with Log Events, default value: `1000`

`sink.pattern` - pattern of streams are subscribed by consumers 

`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

#### Sender settings
`sink.sender.pingPeriodMs` - elastic server ping period, default value: `5000`

`sink.sender.elastic.mergePropertiesTagToRoot` - flag for moving the contents of the properties container to the root of the object, default value: `false`

`sink.sender.elastic.index.policy` - index policy: use index per day or index lifecycle management. Should be one of `DAILY` or `ILM`, default value: `DAILY`

`sink.sender.retryLimit` - count of trying send batch with retryable errors, default value: `3`

`sink.sender.retryOnUnknownErrors` - should retry request to elastic in case of unknown errors, default value: `false`

##### Elastic Client settings
`sink.sender.elastic.client.hosts` - list of elastic hosts

`sink.sender.elastic.client.maxConnections` - maximum connections for underlying http client, default value: `30`

`sink.sender.elastic.client.maxConnectionsPerRoute` - maximum connections per route for underlying http client, default value: `10`

`sink.sender.elastic.client.retryTimeoutMs` - backoff timeout to retry send to elastic, default value: `30000`

`sink.sender.elastic.client.connectionTimeoutMs` - connection timeout of elastic client, default value: `1000`

`sink.sender.elastic.client.connectionRequestTimeoutMs` - timeout for request connection of elastic client, default value: `500`

`sink.sender.elastic.client.socketTimeoutMs` - timeout for response from elastic, default value: `30000`

`sink.sender.elastic.client.redefinedExceptions` - list of errors, which are retryable, but temporarily are treated as non-retryable, list is empty by default

`sink.sender.elastic.client.index.creation.enable` - should create index in case of `index_not_found_exception`, default value: `false`
  
##### Leprosery settings
`sink.sender.leprosery.enable` - flag for enable resending non-retryable error, default value: `false`

`sink.sender.leprosery.stream` - stream name for writing non-retryable errors
 
`sink.sender.leprosery.apiKey` - key for writing non-retryable errors

`sink.sender.leprosery.index`- index name for writing non-retryable errors

`sink.sender.leprosery.gate.client.urls` - list of Gate urls

`sink.sender.leprosery.gate.client.requestTimeout` - timeout (ms) of response from gate client, default value: 3000

`sink.sender.leprosery.gate.client.connectionTimeout` - timeout (ms) of try connecting to host, if exceeded expectation then try to another host, default value: 30000

`sink.sender.leprosery.gate.client.connectionCount` - count of simultaneous connections, default value: 1000

`sink.sender.leprosery.gate.client.greyListElementsRecoveryTimeMs` - period (ms) in grey list

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
application.host=0.0.0.0
application.port=6501

sink.poolSize=3
sink.pollTimeoutMs=5000
sink.batchSize=10000
sink.pattern=logs_*

sink.consumer.bootstrap.servers=localhost:9092
sink.consumer.max.partition.fetch.bytes=52428800
sink.consumer.max.poll.interval.ms=370000
sink.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

sink.sender.pingPeriodMs=60000
sink.sender.elastic.mergePropertiesTagToRoot=true
sink.sender.elastic.index.policy=DAILY
sink.sender.retryLimit=2
sink.sender.retryOnUnknownErrors=true

sink.sender.elastic.client.hosts=localhost:9201
sink.sender.elastic.client.maxConnections=30
sink.sender.elastic.client.maxConnectionsPerRoute=10
sink.sender.elastic.client.retryTimeoutMs=120000
sink.sender.elastic.client.connectionTimeoutMs=1000
sink.sender.elastic.client.connectionRequestTimeoutMs=500
sink.sender.elastic.client.socketTimeoutMs=120000
sink.sender.elastic.client.index.creation.enable=false

sink.sender.leprosery.enable=false
sink.sender.leprosery.stream=some-dlq-stream-name
sink.sender.leprosery.apiKey=some-dlq-stream-key
sink.sender.leprosery.index=some-dlq-index-pattern
sink.sender.leprosery.gate.client.urls=http://localhost:6306
sink.sender.leprosery.gate.client.requestTimeout=3000
sink.sender.leprosery.gate.client.connectionTimeout=5000
sink.sender.leprosery.gate.client.connectionCount=1000
sink.sender.leprosery.gate.client.greyListElementsRecoveryTimeMs=6000

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
