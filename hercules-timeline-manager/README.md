# Hercules Timeline Manager
Timeline Manager is used for create and delete timeline in Apache Cassandra.

## Settings
Application is configured through properties file.

### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.nodes` - default values: `127.0.0.1`

`cassandra.port` - default value: `9042`

`cassandra.keyspace` - default value: `hercules`

`cassandra.readTimeoutMs` - default value: `12000`

### Apache Curator settings
See Apache Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### HTTP Server settings
`http.server.host` - server host

`http.server.port` - server port

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

## Command line
`java $JAVA_OPTS -jar hercules-timeline-manager.jar application.properties=file://path/to/file/application.properties`

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Timeline Manager uses Timeline's metadata from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.

### `application.properties` sample:
```properties
cassandra.nodes=localhost
cassandra.port=9042
cassandra.keyspace=hercules
cassandra.readTimeoutMs=12000

curator.connectString=localhost:2181,localhsot:2182,localhsot:2183
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=3
curator.retryPolicy.maxSleepTime=3000

http.server.host=0.0.0.0
http.server.port=6508

context.instance.id=1
context.environment=dev
context.zone=default
```
