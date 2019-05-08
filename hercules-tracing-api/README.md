# Hercules Tracing API
## Description

Tracing API is an HTTP-service allowing to find tracings in storage by traceId or traceId+parentSpanId.

API requests will return JSON as the response.

## API methods

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

**Response properties:**

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

### Ping
**Description:** The method indicates that the service is working. 

**Method:** `GET`

**URL:** `/ping`

**Request example:**

```Request
GET /ping HTTP/1.1
```

**Response codes:**

`200` - the service is working

### About

**Description:** The method provides information about the service.

**Method:** `GET`

**URL:** `/about`

**Request example:**

```Request
GET /about HTTP/1.1
```

**Response codes:**

`200` - the service is working

**Response properties:**

ContentType: application/json

**Response body example:**
```response
{
    "applicationName": "Hercules tracing API",
    "applicationId": "tracing-api",
    "version": "0.20.1-SNAPSHOT",
    "commitId": "e840d62543f70b4d469dc848beaf9c7a56d05b56",
    "environment": "production",
    "zone": "devlocal",
    "hostName": "K1805018",
    "instanceId": "1"
}
```

## Settings
Application is configured through properties file.

### HTTP Server settings
`http.server.host` - server host, default value: `0.0.0.0`

`http.server.port` - server port

### Apache Curator settings
See Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### Application context settings
`context.environment` - id of environment

`context.zone` - id of zone

`context.instance.id` - id of instance

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.nodes` - default value: `127.0.0.1`
                                                  
`cassandra.port` - default value: `9042`
                                                  
`cassandra.keyspace` - default value: `hercules`
                                                  
`cassandra.readTimeoutMs` - default value: `12000`

## Command line
`java $JAVA_OPTS -jar hercules-sentry-sink.jar application.properties=file://path/to/properties/file`

## Quick start
### Initialization

Table `tracing_spans` for tracing spans should be created.

### `application.properties` sample:
```properties
http.server.port=6312
http.server.host=0.0.0.0

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000

cassandra.keyspace=test
cassandra.connectionsPerHostLocal=4
cassandra.connectionsPerHostRemote=2
cassandra.maxRequestsPerConnectionLocal=1024
cassandra.maxRequestsPerConnectionRemote=256
cassandra.nodes=localhost
cassandra.port=9042
cassandra.readTimeout=12000

context.instance.id=1
context.zone=devlocal
context.environment=production

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=vostok.hercules
metrics.period=5
```
