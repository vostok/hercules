# Hercules Stream Api
Stream Api is used for reading streams from Apache Kafka.

## API methods

[swagger documentation](../docs/swagger/stream-api-swagger2.yml)

### Ping

**Description:** The method to ping service.

**Method:** `GET`

**URL:** `/ping`

**Response codes:**

`200` - successfully ping.

### About

**Description:** The method to get service information.

**Method:** `GET`

**URL:** `/about`

**Response codes:**

`200` - successfully getting service information.

**Response body:**

Response body contains information about service:

```
applicationName - human readable application name
applicationId - robot readable application name
version - application version
commitId - commit id
environment - environment in which service is running (production, testing etc.)
zone - datacenter in which instance is located
hostName - server host name
instanceId - instance identifier
```
### Read 

**Description:** The method to read stream content.

**Method:** `POST`

**URL:** `/stream/read`

**Request headers**

`apiKey`  
The API Key with read access to the stream is specified.  
*Required*

`Content-Type: application/octet-stream`  
*Required*

`Accept-Encoding: lz4`  
If client supports LZ4-compression. Then server MAY compress response body using LZ4-compression.  
*Optional*

**Query parameters:**

`stream` - the name of stream. Required.

`shardIndex` - the logical shard index. Starts with `0` up to `shardCount - 1`. Required.

`shardCount` - the total logical shards. Should be positive. Required.

`take` - maximum events to read. Required.

`timeoutMs` *(optional)* - maximum time to read in milliseconds is in range `[500, 10 000]`, default value: `1 000` ms.

**Request body:**

Optional read state by shards `State` is provided in the request body as follows:
```
RequestBody     State
State           Count, ShardState*
Count           Integer
ShardState      Partition, Offset
Partition       Integer
Offset          Long
```

**Response codes:**

`200` - successfully read stream and return it's content in response body.

`400` - bad request.

`401` - no API key is provided or it is invalid.

`403` - read rules for this apiKey is absent.

`404` - the stream not found.

`411` - can't get Content-Length value.

`500` - internal service error.

**Response headers:**

`Content-Type: application/octet-stream`

`Content-Length`

`Content-Encoding: lz4`  
If response body is compressed. See Request headers for details.

`Original-Content-Length`  
If `Content-Encoding` is used. Value MUST equal original content length (before compression).

**Response body:**

Response body contains new read state and events as follows:
```
ResponseBody    State, Events
Events          Count, Event*
``` 

### Seek to end

**Description:** The method to seek the end of the stream.

**Method:** `GET`

**URL:** `/stream/seekToEnd`

**Request headers**

`apiKey` - the API Key with read access to the stream is specified. Required.

`ContentType: application/octet-stream`

**Query parameters:**

`stream` - the name of stream. Required.

`shardIndex` - the shard index. Starts with `0` up to `shardCount - 1`. Required.

`shardCount` - the total shard count. Should be positive. Required.

**Response codes:**

`200` - successfully seek the end of the stream and return it as read state in response body.

`400` - bad request.

`401` - read rules for this apiKey is absent.

`403` - the stream cannot be accessed with provided API key.

`404` - the stream not found.

`500` - internal service error.

**Response headers:**

`Content-Type: application/octet-stream`

`Content-Length`

**Response body:**

Response body contains read state as follows:

```
ResponseBody    State
```

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Application context settings
`context.environment` - id of environment

`context.zone` - id of zone

`context.instance.id` - id of instance

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

### Http Server settings
`http.server.ioThreads` - the number of IO threads. IO threads are used to read incoming requests and perform non-blocking tasks. One IO thread per CPU core should be enough. Default value is implementation specific.

`http.server.workerThreads` - the number of worker threads. Worker threads are used to process long running requests and perform blocking tasks. Default value is implementation specific.

`http.server.rootPath` - base url, default value: `/`

### Stream API settings
`stream.api.pool.size` - consumers pool size. Default value: `4`.

#### Kafka Consumer settings
Consumer settings have base scope `stream.api.pool.consumer`.
Thus, Consumer's config property `bootstrap.server` is accessed via `stream.api.pool.consumer.bootstrap.servers`.

See the list of supported config properties in Apache Kafka documentation. Main properties are presented below.

`stream.api.pool.consumer.bootstrap.servers` - see KafkaConsumer's `bootstrap.servers` property. Required.

`stream.api.pool.consumer.max.poll.records` - see KafkaConsumer's `max.poll.records` property. Default value: `10000`.

`stream.api.pool.consumer.metric.reporters` - a list of classes to use as metrics reporters

#### Stream API metrics settings
Stream API supports sampling metrics of a stream reading.
Settings for them have base scope `stream.api.stream.read.request.processor.metrics.sampling`.

`stream.api.stream.read.request.processor.metrics.sampling.enable` - enable sampling metrics if `true`. Default value: `false`.

`stream.api.stream.read.request.processor.metrics.sampling.timeout.ms` - sample requests if `timeoutMs` query parameter is less or equal to this value. Default value: `1000`.

`stream.api.stream.read.request.processor.metrics.sampling.response.data.size.bytes` - sample requests if response data size in bytes is less or equal to this value. Default value: `10485760`.

## Command line
`java $JAVA_OPTS -jar hercules-stream-api.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Stream Api uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6307

context.environment=dev
context.zone=default
context.instance.id=1

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

http.server.ioThreads=8
http.server.workerThreads=32
http.server.rootPath=/

stream.api.pool.size=4
stream.api.pool.consumer.bootstrap.servers=localhost:9092
stream.api.pool.consumer.max.poll.records=10000
stream.api.pool.consumer.metric.reporters=ru.kontur.vostok.hercules.kafka.util.metrics.GraphiteReporter

stream.api.stream.read.request.processor.metrics.sampling.enable=true
stream.api.stream.read.request.processor.metrics.sampling.timeout.ms=1000
stream.api.stream.read.request.processor.metrics.sampling.response.data.size.bytes=10485760
```
