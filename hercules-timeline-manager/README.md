# Hercules Timeline Manager
Timeline Manager is used for create and delete timeline in Apache Cassandra.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Apache Cassandra settings
See Apache Cassandra Config from Apache Cassandra documentation. Main settings are presented below.

`cassandra.dataCenter` - local Cassandra DC, default value: `datacenter1`

`cassandra.nodes` - nodes of Cassandra in form `<host>[:port][,<host>[:port],...]`, default value: `127.0.0.1`,
also, default port value is `9042`

`cassandra.keyspace` - default value: `hercules`

`cassandra.requestTimeoutMs` - default value: `12000`

### Apache Curator settings
See Apache Curator Config from Apache Curator documentation. Main settings are presented below.

`curator.connectString` - default value: `localhost:2181`

`curator.connectionTimeout` - default value: `10000`

`curator.sessionTimeout` - default value: `30000`

`curator.retryPolicy.baseSleepTime` - default value: `1000`

`curator.retryPolicy.maxRetries` - default value: `5`

`curator.retryPolicy.maxSleepTime` - default value: `8000`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - id of environment

`context.zone` - id of zone

### Http Server settings
`http.server.ioThreads` - the number of IO threads. Default value: `1`.

`http.server.workerThreads` - the number of worker threads. Default value: `1`.

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
application.host=0.0.0.0
application.port=6508

cassandra.dataCenter=datacenter1
cassandra.nodes=localhost:9042,localhost:9043,localhost:9044
cassandra.keyspace=hercules
cassandra.requestTimeoutMs=12000

curator.connectString=localhost:2181,localhsot:2182,localhsot:2183
curator.connectionTimeout=10000
curator.sessionTimeout=30000
curator.retryPolicy.baseSleepTime=1000
curator.retryPolicy.maxRetries=3
curator.retryPolicy.maxSleepTime=3000

context.instance.id=1
context.environment=dev
context.zone=default

http.server.ioThreads=1
http.server.workerThreads=1
```
