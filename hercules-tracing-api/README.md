# Hercules Tracing API
## Description

Tracing API is an HTTP-service allowing to find tracings in storage by traceId or traceId+parentSpanId.

API requests will return JSON as the response.

## API methods

[swagger documentation](../docs/swagger/tracing-api-swagger2.yml)

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

### Get trace

**Description:** The method provides traces by traceId or traceId+parentSpanId from Cassandra.
A client specifies `traceId` to get traces matching it.
The client can additionally specify `parentSpanId` and get a trace mathing these `traceId` and `parentSpanId`.

**Method:** `GET`

**URL:** `/trace`

**Query parameters:**

`traceId` - unique identifier of the trace containing the span (GUID).

`parentSpanId` *(optional)* - unique identifier of the parent span in the tree (GUID) 

`limit` *(optional)* - count limit of traces in response.

`pagingState` *(optional)* - paging state, is used for paging of result.

**Request example:**

```Request
GET /trace?traceId=1a2b3c4d-9bec-40b0-839b-cc51e2abcdef&parentSpanId=abcdef12-acde-4675-9322-f96cc1234567 HTTP/1.1
```

**Response codes:**

`200` - a response as JSON is received

`400` - the request has incorrect values

**Response headers:**

ContentType: application/json

**Response body example:**
```response
{
  "result":
    [
      {
        "traceId":"1a2b3c4d-9bec-40b0-839b-cc51e2abcdef",
        "spanId":"7a99a678-def0-4567-abad-ba7fc38ffa13",
        "endTimestampUtcOffset":18000,
        "beginTimestampUtcOffset":18000,
        "endTimestampUtc":1555920934013,
        "beginTimestampUtc":1555920933913,
        "annotations":
          {
            "SomeKey":"Some brilliant string"
          },
        "parentSpanId":"abcdef12-acde-4675-9322-f96cc1234567"
      }
    ]
}
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

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Tracing Reader settings
`reader.source` - traces source (`CASSANDRA` or `CLICKHOUSE`), default value: `CASSANDRA`

#### Cassandra Tracing Reader settings
`reader.table` - table name in Cassandra

##### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`reader.cassandra.dataCenter` - local Cassandra DC, default value: `datacenter1`

`reader.cassandra.nodes` - nodes of Cassandra in form `<host>[:port][,<host>[:port],...]`, default value: `127.0.0.1`,
also, default port value is `9042`

`reader.cassandra.keyspace` - keyspace in Cassandra, default value: `hercules`

`reader.cassandra.requestTimeoutMs` - request to Cassandra timeout, default value: `12000`

`reader.cassandra.connectionsPerHostLocal` - connections per local Cassandra node (see Cassandra docs for details), default value: `4`

`reader.cassandra.connectionsPerHostRemote` - connections per remote Cassandra node (see Cassandra docs for details), default value: `2`

`reader.cassandra.maxRequestsPerConnection` - max requests per connection, default value: `1024`

`reader.cassandra.consistencyLevel` - consistency level (see Cassandra docs for details), default value: `QUORUM`

#### ClickHouse Tracing Reader settings
`reader.table` - table name in ClickHouse

##### ClickHouse settings
`reader.clickhouse.nodes` - ClickHouse `node:port` comma-separated list,
default value: `localhost:8123`

`reader.clickhouse.db` - database name in ClickHouse, default value: `default`

`reader.clickhouse.validationIntervalMs` - interval in millis to validate open connections and re-create failed ones, default value: `10000`

`reader.clickhouse.properties` - base scope for ClickHouse connection properties, see JDBC driver docs for details

### Http Server settings
`http.server.ioThreads` - the number of IO threads. IO threads are used to read incoming requests and perform non-blocking tasks. One IO thread per CPU core should be enough. Default value is implementation specific.

`http.server.workerThreads` - the number of worker threads. Worker threads are used to process long running requests and perform blocking tasks. Default value is implementation specific.

`http.server.rootPath` - base url, default value: `/`

## Command line
`java $JAVA_OPTS -jar hercules-sentry-sink.jar application.properties=file://path/to/properties/file`

## Quick start
### Initialization

Table `tracing_spans` for tracing spans should be created.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6310

context.environment=dev
context.zone=default
context.instance.id=1

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

reader.source=CASSANDRA
reader.table=tracing_spans
reader.cassandra.dataCenter=datacenter1
reader.cassandra.nodes=localhost:9042,localhost:9043,localhost:9044
reader.cassandra.keyspace=hercules_traces
reader.cassandra.requestTimeoutMs=12000
reader.cassandra.connectionsPerHostLocal=4
reader.cassandra.connectionsPerHostRemote=2
reader.cassandra.maxRequestsPerConnection=1024
reader.cassandra.consistencyLevel=QUORUM

http.server.ioThreads=8
http.server.workerThreads=32
http.server.rootPath=/
```
