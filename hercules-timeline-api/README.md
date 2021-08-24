# Hercules Timeline Api
Timeline Api is used for reading timelines from Apache Kafka.

## API methods

[swagger documentation](../docs/swagger/timeline-api-swagger2.yml)

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

## Read

**Description:** The method to read the timeline content.

**Method:** `POST`

**URL:** `/timeline/read`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey ".  
The API Key with read access to the timeline is specified.  
*Required*

`apiKey`  
The API Key with read access to the timeline is specified.  
*Deprecated:* use header `Authorization` instead.

`ContentType: application/octet-stream`

**Query parameters:**

`timeline` - the name of timeline. Required.

`shardIndex` - the logical shard index. Starts with `0` up to `shardCount - 1`. Required.

`shardCount` - the total logical shards. Should be positive. Required.

`take` - maximum events to read. Required.

`from` - lower timestamp bound in 100-ns ticks from Unix epoch. Required.

`to` - upper timestamp bound exclusive in 100-ns ticks from Unix epoch. Required.

**Request body:**

Optional read state by shards `State` is provided in the request body as follows:
```
TimelineState		Count, TimelineSliceState*
Count			    Integer
TimelineSliceState	Slice, Offset, EventId
Slice			    Integer
Offset			    Long
EventId			    Long, Long
```

**Response codes:**

`200` - successfully read timeline and return it's content in response body.

`400` - bad request.

`401` - read rules for this apiKey is absent.

`403` - the timeline cannot be accessed with provided API key.

`404` - the timeline not found.

`411` - can't get Content-Length value.

`500` - internal service error.

**Response headers:**

ContentType: application/octet-stream

**Response body:**

Response body contains new read state and events as follows:
```
ResponseBody    TimelineState, Events
Events          Count, Event*
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

### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.dataCenter` - local Cassandra DC, default value: `datacenter1`

`cassandra.nodes` - nodes of Cassandra in form `<host>[:port][,<host>[:port],...]`, default value: `127.0.0.1`,
also, default port value is `9042`

`cassandra.keyspace` - default value: `hercules`

`cassandra.requestTimeoutMs` - default value: `12000`

`cassandra.auth.enable` - if Cassandra requires authentication then set this property value to `true`
and specify credential in the respective properties, default value: `false`

`cassandra.auth.provider.username` - username which is needed for Cassandra authentication.
*Required* if `cassandra.auth.enable` is set to `true`, otherwise value is *ignored*.

`cassandra.auth.provider.password` - password which is needed for Cassandra authentication.
*Required* if `cassandra.auth.enable` is set to `true`, otherwise value is *ignored*.

`cassandra.auth.provider.class` - name of the class which is needed for Cassandra authentication.
Only classes that implements `com.datastax.driver.core.AuthProvider` should be specified, default value: `PlainTextAuthProvider`

### HTTP Server settings
`http.server.ioThreads` - the number of IO threads. IO threads are used to read incoming requests and perform non-blocking tasks. One IO thread per CPU core should be enough. Default value is implementation specific.

`http.server.workerThreads` - the number of worker threads. Worker threads are used to process long running requests and perform blocking tasks. Default value is implementation specific.

`http.server.rootPath` - base url, default value: `/`

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
application.host=0.0.0.0
application.port=6308

context.instance.id=1
context.environment=dev
context.zone=default

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000
cassandra.auth.enable=false

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

cassandra.dataCenter=datacenter1
cassandra.nodes=localhost:9042,localhost:9043,localhost:9044
cassandra.keyspace=hercules
cassandra.requestTimeoutMs=12000

http.server.ioThreads=8
http.server.workerThreads=32
http.server.rootPath=/
```
