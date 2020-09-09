# Hercules Elastic Adapter
Elastic Adapter implements Elasticsearch API for indexing documents.
Elastic Adapter receives log events from Elasticsearch compatible clients and transmit those events to Hercules Gate.

## API methods
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

### Bulk API

**Description:** Bulk API of Elasticsearch.
See Elasticsearch [docs](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html) for details.

**Method:** `POST`

**URL:**  
`/_bulk`  
`/:index/_bulk`  
`/:index/_doc/_bulk`

**Request headers**

`ContentType: application/x-ndjson` or `application/json`

**Path parameters:**

`index` - Elasticsearch index name. Used as default index.

**Request body:**

```
{ "index": { /* index action content here */ } }
{ /* Log event here */ }
...
```

**Response codes:**

`200` - successfully processed.

`400` - bad request.

### Index API

**Description:** Index API of Elasticsearch.
See Elasticsearch [docs](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html) for details.

**Method:** `POST`

**URL:** `/:index/_doc/`

**Request headers**

`ContentType: application/json`

**Path parameters:**

`index` - Elasticsearch index name.

**Request body:**

```
{ /* Log event here */ }
```
Required field `@timestamp` should contain log event timestamp in ISO 8601 date and time format (`YYYY-MM-DDThh:mm:ss[.sss][±hh:mm|Z]`).

**Response codes:**

`200` - successfully processed.

`400` - bad request.

`404` - index not found.

`503` - gate is unavailable.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### HTTP Server settings
HTTP Server binds on host:port are defined in Main Application settings.

`http.server.maxContentLength` - max Content-Length in POST-request

`http.server.connection.threshold` - maximum active http connections, default value: `100000`

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance to which metrics are sent, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix added to metric name

`metrics.period` - the period with which metrics are sent to graphite, default value: `60`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

### Gate Client settings
`gate.client.apiKey` - Hercules API key for log streams, required

`gate.client.urls` - Gate topology, required

### Index Manager settings
`index.manager.config.path` - the path to the per index configuration, default value: `file://indices.json`

## Command line
`java $JAVA_OPTS -jar hercules-elastic-adapter.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Gate uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6401

http.server.maxContentLength=25165824
http.server.connection.threshold=100000

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default

gate.client.apiKey=api_key_cef2e9a230dc434ebdfa7db549bd3138
gate.client.urls=http://localhost:6306

index.manager.config.path=file://indices.json
```

### `indices.json` sample
```json
{
  "test_index": {
    "stream": "test_stream",
    "properties": {
      "properties/project": "test_project"
    },
    "indexPath": "properties/elk-index",
    "timestampFormat": "ISO_DATE_TIME",
    "mappingFile": "resource://log-event.reverse.mapping"
  }
}
```
