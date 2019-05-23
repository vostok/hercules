# Hercules Management Api
Management Api is used for working with streams, timelines, rules and blacklist in Apache Kafka.
Management Api provides following opportunities:
* Streams:
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for create stream;
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for delete stream;
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for change TTL of stream;
  * Gives the command to [Stream Manager](../hercules-stream-manager/README.md) for increase partitions count in stream;
  * Show all streams;
  * Get info about stream.
  
* Timelines:
  * Gives the command to [Timeline Manager](../hercules-timeline-manager/README.md) for creat timeline;
  * Gives the command to [Timeline Manager](../hercules-timeline-manager/README.md) for delete timeline;
  * Gives the command to [Timeline Manager](../hercules-timeline-manager/README.md) for change TTL of timeline;
  * Show all timelines;
  * Get info about timeline.

* Rules:
  * Create rule;
  * Show all rules.
  
* Blacklist:
  * Add apiKey to blacklist;
  * Remove apiKey from blacklist;
  * Show all records in blacklist.
  
## API methods

[swagger documentation](../docs/management-api/management-api-swagger2.yml)

## Default

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

## Stream

### Create stream

**Description:** The method for creation stream.

**Method:** `POST`

**URL:** `/streams/create`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Request body:**

```
"type": type of stream
"name": name of stream
"partitions": count of partition in stream
"shardingKey": sharding keys array
"ttl": time to live
```

**Request body example:**

```
{
  "type": "base",
  "name": "project_test_stream_0",
  "partitions": 1,
  "shardingKey": [],
  "ttl": 3600000
}
```

**Response codes:**

`200` - successfully creation of stream.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`409` - conflict. Entity already exists.

### Delete stream

**Description:** The method for deletion stream.

**Method:** `POST`

**URL:** `/streams/delete`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Query parameters:**

`stream` - the name of stream. Required.

**Response codes:**

`200` - successfully deletion of stream.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source stream not found.

### Show streams

**Description:** The method for show all streams.

**Method:** `GET`

**URL:** `/streams/list`

**Request headers**

`apiKey` - any API Key. Required.

**Response headers:**

ContentType: application/json

**Response body example**
```
{
    "stream_1",
    "stream_2",
    "stream_3"
}
```

**Response codes:**

`200` - list of streams.

`400` - bad request.

`401` - apiKey is absent.

### Increase stream partition count

**Description:** The method for increase partition count in stream.

**Method:** `POST`

**URL:** `/streams/increasePartitions`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Query parameters:**

`stream` - the name of stream. Required.

`newPartitions` - new count of partition in stream. Required.

**Response codes:**

`200` - successfully increase count of partition in stream.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source stream not found.

### Change ttl of stream

**Description:** The method for changing ttl of stream.

**Method:** `POST`

**URL:** `/streams/changeTtl`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Query parameters:**

`stream` - the name of stream. Required.

`newTtl` - new ttl of stream. Required.

**Response codes:**

`200` - successfully change ttl of stream.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source stream not found.

### Show information about stream

**Description:** The method for show information about stream.

**Method:** `GET`

**URL:** `/streams/info`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Query parameters:**

`stream` - the name of stream. Required.

**Response headers:**

ContentType: application/json

**Response body example**
```
{
    "type": "base",
    "name": "stream",
    "partitions": 3,
    "shardingKey": [],
    "ttl": 86300000
}
```

**Response codes:**

`200` - information about stream.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source stream not found.

## Timeline

### Create timeline

**Description:** The method for creation timeline.

**Method:** `POST`

**URL:** `/timelines/create`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Request body:**

```
"name": name of timeline
"slices": count of slice
"shardingKey": sharding keys array
"ttl": time to live
"timetrapSize": timetrap size
"streams": streams in timeline
"filters": filters array
```

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
  "filters": [
    "filter"
  ]
}
```

**Response codes:**

`200` - successfully creation of timeline.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`409` - conflict. Entity already exists.

### Delete timeline

**Description:** The method for deletion timeline.

**Method:** `POST`

**URL:** `/timelines/delete`

**Request headers**

`apiKey` - the API Key with manage access to the timeline is specified. Required.

**Query parameters:**

`timeline` - the name of timeline. Required.

**Response codes:**

`200` - successfully deletion of timeline.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source timeline not found.

### Show timelines

**Description:** The method for show all timelines.

**Method:** `GET`

**URL:** `/timelines/list`

**Request headers**

`apiKey` - any API Key. Required.

**Response headers:**

ContentType: application/json

**Response body example**
```
{
    "timeline_1",
    "timeline_2",
    "timeline_3"
}
```

**Response codes:**

`200` - list of timelines.

`400` - bad request.

`401` - apiKey is absent.

### Change ttl of timeline

**Description:** The method for change ttl of timeline.

**Method:** `POST`

**URL:** `/timelines/changeTtl`

**Request headers**

`apiKey` - the API Key with manage access to the stream is specified. Required.

**Query parameters:**

`timeline` - the name of timeline. Required.

`newTtl` - new ttl of timeline. Required.

**Response codes:**

`200` - successfully change ttl of timeline.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source timeline not found.

### Show information about timeline

**Description:** The method for show information about timeline.

**Method:** `GET`

**URL:** `/timelines/info`

**Request headers**

`apiKey` - the API Key with manage access to the timeline is specified. Required.

**Query parameters:**

`timeline` - the name of timeline. Required.

**Response headers:**

ContentType: application/json

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
    "filters": []
}
```

**Response codes:**

`200` - information about timeline.

`400` - bad request.

`401` - management rules for this apiKey is absent.

`403` - forbidden for this API-key.

`404` - source timeline not found.

## Rules

### Set rules

**Description:** The method for set rules for ApiKey.

**Method:** `POST`

**URL:** `/rules/set`

**Request headers**

`masterApiKey` - the API key for rules management. Required.

**Query parameters:**

`key` - the API key for which rule is set. Required.

`pattern` - pattern for stream or timeline names. Required.

`rights` - combination of read, write and modified rights. Required. 
Available values : ---, r--, -w-, --m, rw-, r-m, -wm, rwm.

**Response codes:**

`200` - successfully set rules.

`400` - bad request.

`401` - unauthorized.

### Show rules

**Description:** The method for show all rules.

**Method:** `GET`

**URL:** `/rules/list`

**Request headers**

`masterApiKey` - the API key for rules management. Required.

**Response headers:**

ContentType: application/json

**Response body example**
```
{
    "apiKey_1.pattern_1.manage",
    "apiKey_2.pattern_2.read",
    "apiKey_3.pattern_3.write"
}
```

**Response codes:**

`200` - list of rules.

`400` - bad request.

`401` - unauthorized.

## Blacklist

### Add apiKey to blacklist

**Description:** The method for add apiKey to blacklist.

**Method:** `POST`

**URL:** `/blacklist/add`

**Request headers**

`masterApiKey` - the API key for rules management. Required.

**Query parameters:**

`key` - the API key for adding to blacklist. Required.

**Response codes:**

`200` - successfully add apiKey to blacklist.

`400` - bad request.

`401` - unauthorized.

### Remove apiKey from blacklist

**Description:** The method for removing apiKey from blacklist.

**Method:** `POST`

**URL:** `/blacklist/remove`

**Request headers**

`masterApiKey` - the API key for rules management. Required.

**Query parameters:**

`key` - the API key for removing from blacklist. Required.

**Response codes:**

`200` - successfully removing apiKey from blacklist.

`400` - bad request.

`401` - unauthorized.

### Show apiKeys in blacklist

**Description:** The method for show all apiKey from blacklist.

**Method:** `GET`

**URL:** `/blacklist/list`

**Request headers**

`masterApiKey` - the API key for rules management. Required.

**Response headers:**

ContentType: application/json

**Response body example**
```
{
    "apiKey_1",
    "apiKey_2",
    "apiKey_3"
}
```

**Response codes:**

`200` - all apiKeys in blacklist.

`400` - bad request.

`401` - unauthorized.

## Settings
Application is configured through properties file.

### Apache Curator settings
See Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### Apache Kafka settings
See Apache Kafka Config from Apache Kafka documentation. Main settings are presented below.

`kafka.bootstrap.servers`

`kafka.acks`

`kafka.retries`

`kafka.batch.size`

`kafka.linger.ms`

`kafka.buffer.memory`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Http Server settings
`http.server.host` - server host, default value: `0.0.0.0`

`http.server.port` - server port, default value: `6309`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

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

curator.connectString=localhost:2181
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=5
curator.retryPolicy.maxSleepTime=8000

kafka.bootstrap.servers=localhost:9092
kafka.acks=all
kafka.retries=0
kafka.batch.size=16384
kafka.linger.ms=1
kafka.buffer.memory=33554432

keys=123,456

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

http.server.host=0.0.0.0
http.server.port=6309

context.instance.id=1
context.environment=dev
context.zone=default
```
