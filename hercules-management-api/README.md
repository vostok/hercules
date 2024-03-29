# Hercules Management API
Management API is used for working with streams, timelines, rules and blacklist.
Management API provides following methods:
* Streams:
  * Create / delete / change Streams using [Stream Manager](../hercules-stream-manager/README.md);
  * Show all streams;
  * Get info about stream;
  * Change ttl, description and partition count of stream.
  
* Timelines:
  * Create / delete / change Timelines using [Timeline Manager](../hercules-timeline-manager/README.md);
  * Show all Timelines;
  * Get info about Timeline.

* Rules:
  * Set rules for API Key;
  * Show all rules.
  
* Blacklist:
  * Add API Key to blacklist;
  * Remove API Key from blacklist;
  * Show all records in blacklist.
  
## API methods

[swagger documentation](../docs/swagger/management-api-swagger2.yml)

## Default

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

## Stream

### Create stream

**Description:** The method to create stream.

**Method:** `POST`

**URL:** `/streams/create`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`  
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

`Content-Type: application/json`  
*Required*

**Query parameters**

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Request body:**

Request body contains JSON-object with following properties:
- `type` - the Stream type (`base` or `derived`), *string*
- `name` - the Stream name, *string*
- `partitions` - the Stream partitions count, *integer*
- `shardingKey` - Sharding key is array of [HPath](../hercules-protocol/doc/h-path.md), *array of strings*
- `ttl` - events time to live (TTL) in milliseconds, *integer*
- `description` - the Stream description, *string*
- `streams` - streams to fill target derived stream (for `derived` streams only), *array of strings*
- `filters` - filters array (for `derived` streams only, *array of objects*

**Request body example:**

```
{
  "type": "base",
  "name": "project_test_stream",
  "partitions": 1,
  "shardingKey": [],
  "ttl": 3600000,
  "description": "Test stream"
}
```

**Response codes:**

| Code  | Description                                                            |
|-------|------------------------------------------------------------------------|
| `200` | Successfully created stream.                                           |
| `400` | Invalid request body or parameters (see response message for details). |
| `401` | apiKey is not valid or absent.                                         |
| `403` | Access is denied (check if apiKey has appropriate access rights).      |
| `408` | Requested operation took too long.                                     |
| `409` | Stream with such a name already exists.                                |
| `411` | Content length must be specified.                                      |
| `500` | Internal error while processing request.                               |

### Delete stream

**Description:** The method to delete stream.

**Method:** `POST`

**URL:** `/streams/delete`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`stream` - the name of stream. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Successfully deleted stream.                                      |
| `400` | Parameter 'stream' is invalid.                                    |
| `401` | apiKey is not valid or absent.                                    |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source stream not found.                                          |
| `408` | Requested operation took too long.                                |
| `500` | Internal error while processing request.                          |

### Show streams

**Description:** The method to show all streams.

**Method:** `GET`

**URL:** `/streams/list`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
Any valid API key.  
*Required*

`apiKey` or `masterApiKey`    
Any valid API Key.  
*Deprecated:* use header `Authorization` instead.

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "stream_1",
    "stream_2",
    "stream_3"
}
```

**Response codes:**

| Code  | Description                                                                  |
|-------|------------------------------------------------------------------------------|
| `200` | List of streams.                                                             |
| `401` | Both master api key and api key are either not valid or absent               |
| `500` | Internal error while processing request.                                     |

### Increase stream partition count

**Description:** The method to increase partition count in stream.

**Method:** `POST`

**URL:** `/streams/increasePartitions`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`stream` - the name of stream. *Required*

`newPartitions` - new count of partitions in the stream. The value should be in range `(<current partition count>, 100)`. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                                      |
|-------|----------------------------------------------------------------------------------|
| `200` | Successfully increased count of partition in the stream.                         |
| `400` | Invalid parameters (see response message for details).                           |
| `401` | apiKey is not valid or absent.                                                   |
| `403` | Access is denied (check if apiKey has appropriate access rights).                |
| `404` | Source stream not found.                                                         |
| `408` | Requested operation took too long.                                               |
| `409` | New partitions count cannot be less than or equal to a current partitions count. |
| `500` | Internal error while processing request.                                         |

### Change TTL of stream

**Description:** The method to change TTL of stream.

**Method:** `POST`

**URL:** `/streams/changeTtl`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`stream` - the name of stream. *Required*

`newTtl` - new TTL of stream. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Successfully changed the TTL of the stream.                       |
| `400` | Invalid parameters (see response message for details).            |
| `401` | apiKey is not valid or absent.                                    |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source stream not found.                                          |
| `408` | Requested operation took too long.                                |
| `500` | Internal error while processing request.                          |

### Change description of stream

**Description:** The method to change description of stream.

**Method:** `POST`

**URL:** `/streams/changeDescription`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`stream` - the name of stream. *Required*

`newDescription` - new description of stream. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Successfully changed the TTL of the stream.                       |
| `400` | Invalid parameters (see response message for details).            |
| `401` | apiKey is not valid or absent.                                    |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source stream not found.                                          |
| `408` | Requested operation took too long.                                |
| `500` | Internal error while processing request.                          |

### Show information about stream

**Description:** The method to show information about stream.

**Method:** `GET`

**URL:** `/streams/info`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`stream` - the name of stream. *Required*

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "type": "base",
    "name": "stream",
    "partitions": 3,
    "shardingKey": [],
    "ttl": 86300000,
    "description": "Some stream"
}
```

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Information about stream.                                         |
| `400` | Parameter 'stream' is invalid.                                    |
| `401` | apiKey is not valid or absent.                                    |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source stream not found.                                          |
| `500` | Internal error while processing request.                          |


## Timeline

### Create timeline

**Description:** The method to create timeline.

**Method:** `POST`

**URL:** `/timelines/create`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.   
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

`Content-Type: application/json`  
*Required*

**Query parameters**

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Request body:**

Request body contains JSON-object with following properties:
- `name` - the Timeline name, *string*
- `slices` - slices count, *integer*
- `shardingKey` - Sharding Key is array of [HPath](../hercules-protocol/doc/h-path.md), *array of strings*
- `ttl` - events time to live (TTL) in milliseconds, *integer*
- `timetrapSize` - timetrap size in milliseconds, *integer*
- `streams` - streams to fill target timeline, *array of strings*
- `filters` - filters array, *array of objects*
- `description` - description, *string*

**Request body example:**

```
{
  "name": "timeline",
  "slices": 150,
  "shardingKey": [
    "key"
  ],
  "ttl": 18000000,
  "timetrapSize": 5000,
  "streams": [
    "stream"
  ],
  "filters": [],
  "description": "Some timeline"
}
```

**Response codes:**

| Code  | Description                                                            |
|-------|------------------------------------------------------------------------|
| `200` | Successfully created timeline.                                         |
| `400` | Invalid request body or parameters (see response message for details). |
| `401` | Both masterApiKey and apiKey are either not valid or absent.           |
| `403` | Access is denied (check if apiKey has appropriate access rights).      |
| `408` | Requested operation took too long.                                     |
| `409` | Timeline with such a name already exists.                              |
| `411` | Content length must be specified.                                      |
| `500` | Internal error while processing request.                               |

### Delete timeline

**Description:** The method to delete timeline.

**Method:** `POST`

**URL:** `/timelines/delete`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the timeline.  
*Required*

`apiKey` or `masterApiKey`    
The API Key with manage access to the timeline.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`timeline` - the name of timeline. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Successfully deleted timeline.                                    |
| `400` | Parameter 'timeline' is invalid.                                  |
| `401` | Both masterApiKey and apiKey are either not valid or absent.      |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source timeline not found.                                        |
| `408` | Requested operation took too long.                                |
| `500` | Internal error while processing request.                          |

### Show timelines

**Description:** The method to show all timelines.

**Method:** `GET`

**URL:** `/timelines/list`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
Any valid API key.  
*Required*

`apiKey` or `masterApiKey`  
Any valid API key.  
*Deprecated:* use header `Authorization` instead.

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "timeline_1",
    "timeline_2",
    "timeline_3"
}
```

**Response codes:**

| Code  | Description                                                                    |
|-------|--------------------------------------------------------------------------------|
| `200` | List of timelines.                                                             |
| `401` | Both masterApiKey and apiKey are either not valid or absent.                   |
| `500` | Internal error while processing request.                                       |

### Change TTL of timeline

**Description:** The method to change ttl of timeline.

**Method:** `POST`

**URL:** `/timelines/changeTtl`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the stream.  
*Required*

`apiKey` or `masterApiKey`  
The API Key with manage access to the stream.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`timeline` - the name of timeline. *Required*

`newTtl` - new TTL of timeline. *Required*

`async` - if presented, request will be processed asynchronously. *Optional*

`timeoutMs` - request timeout in milliseconds should be in range `[1000, 30000]`, default value: `15000` ms

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Successfully changed the TTL of the timeline.                     |
| `400` | Invalid parameters (see response message for details).            |
| `401` | Both masterApiKey and apiKey are either not valid or absent.      |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source timeline not found.                                        |
| `408` | Requested operation took too long.                                |
| `500` | Internal error while processing request.                          |


### Show information about timeline

**Description:** The method to show information about timeline.

**Method:** `GET`

**URL:** `/timelines/info`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules apiKey " or "Hercules masterApiKey ".  
The API Key with manage access to the timeline.  
*Required*

`apiKey` or `masterApiKey`  
The API Key with manage access to the timeline.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`timeline` - the name of timeline. *Required*

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "name": "timeline",
    "slices": 3,
    "shardingKey": [],
    "ttl": 3500000,
    "timetrapSize": 1000,
    "streams": [
        "stream"
    ],
    "filters": [],
    "description": "Some timeline"
}
```

**Response codes:**

| Code  | Description                                                       |
|-------|-------------------------------------------------------------------|
| `200` | Information about timeline.                                       |
| `400` | Parameter 'timeline' is invalid.                                  |
| `401` | Both masterApiKey and apiKey are either not valid or absent.      |
| `403` | Access is denied (check if apiKey has appropriate access rights). |
| `404` | Source timeline not found.                                        |
| `500` | Internal error while processing request.                          |

## Rules

### Set rules

**Description:** The method to set rules for ApiKey.

**Method:** `POST`

**URL:** `/rules/set`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules masterApiKey ".  
The API key for rules management.  
*Required*

`masterApiKey`  
The API key for rules management.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`key` - the API key for which rule is set. *Required*
Key must contain only lowercase alphanumeric characters and underscore.
The end of the key must contain an UUID in lowercase without a hyphens.
Example: `key_name_5c43280c246f41f5acdebc31a6436e8c`.

`pattern` - pattern for stream or timeline names. *Required*

`rights` - combination of read, write and manage rights. *Required*  
Available values : ---, r--, -w-, --m, rw-, r-m, -wm, rwm.

**Response codes:**

| Code  | Description                                            |
|-------|--------------------------------------------------------|
| `200` | Successfully set rules.                                |
| `400` | Invalid parameters (see response message for details). |
| `401` | masterApiKey is not valid or absent                    |
| `500` | Internal error while processing request.               |

### Show rules

**Description:** The method to show all rules.

**Method:** `GET`

**URL:** `/rules/list`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules masterApiKey ".  
The API key for rules management.  
*Required*

`masterApiKey`  
The API key for rules management.  
*Deprecated:* use header `Authorization` instead.

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "apiKey_1.pattern_1.manage",
    "apiKey_2.pattern_2.read",
    "apiKey_3.pattern_3.write"
}
```

**Response codes:**

| Code  | Description                                                                |
|-------|----------------------------------------------------------------------------|
| `200` | List of rules.                                                             |
| `401` | masterApiKey is not valid or absent.                                       |
| `500` | Internal error while processing request.                                   |

## Blacklist

### Add apiKey to blacklist

**Description:** The method to add apiKey to blacklist.

**Method:** `POST`

**URL:** `/blacklist/add`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules masterApiKey ".  
The API key for rules management.  
*Required*

`masterApiKey`  
The API key for rules management.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`key` - the API key for adding to blacklist. *Required*

**Response codes:**

| Code  | Description                              |
|-------|------------------------------------------|
| `200` | Successfully send data into stream.      |
| `400` | Parameter 'key' is invalid.              |
| `401` | masterApiKey is not valid or absent.     |
| `500` | Internal error while processing request. |

### Remove apiKey from blacklist

**Description:** The method to remove apiKey from blacklist.

**Method:** `POST`

**URL:** `/blacklist/remove`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules masterApiKey ".  
The API key for rules management.  
*Required*

`masterApiKey`  
The API key for rules management.  
*Deprecated:* use header `Authorization` instead.

**Query parameters:**

`key` - the API key for removing from blacklist. *Required*

**Response codes:**

| Code  | Description                                  |
|-------|----------------------------------------------|
| `200` | Successfully removing apiKey from blacklist. |
| `400` | Parameter 'key' is invalid.                  |
| `401` | masterApiKey is not valid or absent.         |
| `500` | Internal error while processing request.     |

### Show apiKeys in blacklist

**Description:** The method to show all apiKey from blacklist.

**Method:** `GET`

**URL:** `/blacklist/list`

**Request headers**

`Authorization`  
Value should be with prefix "Hercules masterApiKey ".  
The API key for rules management.  
*Required*

`masterApiKey`  
The API key for rules management.  
*Deprecated:* use header `Authorization` instead.

**Response headers:**

`Content-Type: application/json`

**Response body example**
```
{
    "apiKey_1",
    "apiKey_2",
    "apiKey_3"
}
```

**Response codes:**

| Code  | Description                                                            |
|-------|------------------------------------------------------------------------|
| `200` | All apiKeys in blacklist.                                              |
| `401` | masterApiKey is not valid or absent.                                   |
| `500` | Internal error while processing request.                               |

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
See Curator Config from Apache Curator documentation. Main settings are presented below.

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

`http.server.readTimeout` - Configure a read timeout for a socket, in milliseconds.

`http.server.writeTimeout` - Configure a write timeout for a socket, in milliseconds.

`http.server.requestParseTimeout` - The maximum allowed time of reading HTTP request in milliseconds.
`-1` or missing value disables this functionality.

`http.server.idleTimeout` - The idle timeout in milliseconds after which the channel will be closed.
If the underlying channel already has a read or write timeout set the smaller of the two values will be used for read/write timeouts.

`http.server.noRequestTimeout` - The amount of time the connection can be idle with no current requests before it is closed.

### Management API settings
`keys` - master API keys.

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
keys=123,456

application.host=0.0.0.0
application.port=6309

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

http.server.ioThreads=4
http.server.workerThreads=16
http.server.rootPath=/
```
