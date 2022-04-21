# Hercules Elastic Sink
Elastic Sink is used to move Log Events from Kafka to Elasticsearch.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Sink settings
#### Common settings
See Hercules Sink [docs](../hercules-sink/README.md).

#### Filter settings
Properties for each defined filter under the scope `N`, where `N` is a filter number.

`sink.filter.N.class` - filter class (inheritors of the `ru.kontur.vostok.hercules.sink.filter.EventFilter` class).
Properties with key prefix `sink.filter.N.props` are defined for this class.  
See method `createClassInstanceList` in `ru.kontur.vostok.hercules.util.properties.PropertiesUtil` for details.

#### Sender settings
`sink.sender.pingPeriodMs` - elastic server ping period, default value: `5000`

`sink.sender.retryLimit` - count of trying send batch with retryable errors, default value: `3`

`sink.sender.retryOnUnknownErrors` - should retry request to elastic in case of unknown errors, default value: `false`

##### Index Settings
`sink.sender.elastic.index.policy` - index policy: should use static index name, index per day or index lifecycle management. Should be one of `STATIC`, `DAILY` or `ILM`, respectively, default value: `DAILY`

###### [IndexResolver](../hercules-elastic-sink/src/main/java/ru/kontur/vostok/hercules/elastic/sink/index/IndexResolver.java) properties
Properties for each defined resolver under the scope `N`, where `N` is a resolver number.

`sink.sender.elastic.index.resolver.N.class` - filter class (inheritors of the `ru.kontur.vostok.hercules.elastic.sink.index.IndexResolver` class).
Properties with key prefix `sink.sender.elastic.index.resolver.N.props` are defined for this class.  
See method `createClassInstanceList` in `ru.kontur.vostok.hercules.util.properties.PropertiesUtil` for details.

###### [TagsIndexResolver](../hercules-elastic-sink/src/main/java/ru/kontur/vostok/hercules/elastic/sink/index/TagsIndexResolver.java) properties
`sink.sender.elastic.index.resolver.N.props.tags` - the optional tags to build index name if no stored index name from above setting.
Each tag definition should be a valid HPath. Tag is optional if HPath ends with `?`

###### [StaticIndexResolver](../hercules-elastic-sink/src/main/java/ru/kontur/vostok/hercules/elastic/sink/index/StaticIndexResolver.java) properties
`sink.sender.elastic.index.resolver.N.props.index.name` - static index name.

##### Format Settings
`sink.sender.elastic.format.timestamp.enable` - should use event timestamp as field when send to Elastic, default value: `true`

`sink.sender.elastic.format.timestamp.field` - field name for event timestamp, default value: `@timestamp`

`sink.sender.elastic.format.timestamp.format` - timestamp field format is compatible with `java.time.format.DateTimeFormatter`,
default value: `yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX`

`sink.sender.elastic.format.file` - path to the mapping file. Can use `resource://log-event.mapping` if sink processes logs. See [MappingLoader](../hercules-json/src/main/java/ru/kontur/vostok/hercules/json/format/MappingLoader.java) for details, required

`sink.sender.elastic.format.ignore.unknown.tags` - should ignore event tags for which no mapping is specified, default value: `false`

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
  
`sink.sender.elastic.client.compression.gzip.enable` - flag for enable gzip compression when sending to Elastic, default value: `false`

`sink.sender.elastic.client.auth.type` - Elastic client authentication type `BASIC` or `NONE` if authentication isn't used, default value: `NONE`

`sink.sender.elastic.client.auth.basic.username` - username for basic authentication

`sink.sender.elastic.client.auth.basic.password` - password for basic authentication
 
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

sink.filter.0.class=ru.kontur.vostok.hercules.elastic.sink.LogEventFilter

sink.sender.pingPeriodMs=60000

sink.sender.retryLimit=2
sink.sender.retryOnUnknownErrors=true

sink.sender.elastic.index.policy=ILM

sink.sender.elastic.index.resolver.0.class=ru.kontur.vostok.hercules.elastic.sink.index.TagsIndexResolver
sink.sender.elastic.index.resolver.0.props.tags=properties/elk-index
sink.sender.elastic.index.resolver.1.class=ru.kontur.vostok.hercules.elastic.sink.index.TagsIndexResolver
sink.sender.elastic.index.resolver.1.props.tags=properties/project,properties/environment?,properties/subproject?

sink.sender.elastic.format.timestamp.enable=true
sink.sender.elastic.format.timestamp.field=@timestamp
sink.sender.elastic.format.timestamp.format=yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX
sink.sender.elastic.format.file=file://log-event.mapping

sink.sender.elastic.client.hosts=localhost:9201
sink.sender.elastic.client.maxConnections=30
sink.sender.elastic.client.maxConnectionsPerRoute=10
sink.sender.elastic.client.retryTimeoutMs=120000
sink.sender.elastic.client.connectionTimeoutMs=1000
sink.sender.elastic.client.connectionRequestTimeoutMs=500
sink.sender.elastic.client.socketTimeoutMs=120000
sink.sender.elastic.client.index.creation.enable=false
sink.sender.elastic.client.compression.gzip.enable=false


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

### `log-event.mapping` sample:
```text
# Move all tags from properties container to upper level
move properties/* to *

# Render structured exception as string stack trace
transform exception to stackTrace using ru.kontur.vostok.hercules.elastic.sink.format.ExceptionToStackTraceTransformer
```

### `indices.json` sample:
```json
[
  {
    "index": "first-index",
    "tagMaps": [
      {
        "some-tag": "some-value",
        "other-tag": "other-value"
      },
      {
        "some-tag": "alternative-value"
      }
    ]
  },
  {
    "index": "second-index",
    "tagMaps": [
      {
        "some-tag": "for-second-index-value"
      },
      {
        "special-tag": "special-value"
      }
    ]
  }
]
```
