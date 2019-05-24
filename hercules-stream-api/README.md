# Hercules Stream Api
Stream Api is used for reading streams from Apache Kafka.

## API methods

[swagger documentation](../docs/swagger/stream-api-swagger2.yml)

### Ping

**Description:** The method for ping service.

**Method:** `GET`

**URL:** `/ping`

**Response codes:**

`200` - successfully ping.

### About

**Description:** The method for getting service information.

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

**Description:** The method for reading stream content.

**Method:** `POST`

**URL:** `/stream/read`

**Request headers**

`apiKey` - the API Key with read access to the stream is specified. Required.

`ContentType: application/octet-stream`

**Query parameters:**

`stream` - the name of stream. Required.

`shardIndex` - the logical shard index. Starts with `0` up to `shardCount - 1`. Required.

`shardCount` - the total logical shards. Should be positive. Required.

`take` - maximum events to read. Required.

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

ContentType: application/octet-stream

**Response body:**

Response body contains new read state and events as follows:
```
ResponseBody    State, Events
Events          Count, Event*
``` 

### Seek to end

**Description:** The method for seek the end of the stream.

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

ContentType: application/octet-stream

**Response body:**

Response body contains read state as follows:

```
ResponseBody    State
```

## Settings
Application is configured through properties file.

### Kafka Consumer settings
See Consumer's Config from Apache Kafka documentation. Main settings are presented below.

`consumer.bootstrap.servers` - see KafkaConsumer's `bootstrap.servers` property. Required.

`consumer.max.poll.records` - see KafkaConsumer's `max.poll.records` property. Default value: `1000`.

`consumer.poolSize` - consumers pool size. Default value: `4`.

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
`http.server.host` - server host, default value: `0.0.0.0`

`http.server.port` - server port, default value: `6307`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

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
consumer.bootstrap.servers=localhost:9092
consumer.max.poll.records=1000
consumer.poolSize=16

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000

http.server.host=0.0.0.0
http.server.port=6307

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default
```
