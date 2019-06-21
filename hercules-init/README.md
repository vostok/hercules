# Hercules Init
Init is used for infrastructure initialization:
* Creating the keyspace in Cassandra;
* Creating topics in Kafka (hercules_management_kafka and hercules_management_cassandra);
* Creating nodes in ZooKeeper.

## Settings
Application is configured through properties file.

### ZooKeeper settings
See Apache Curator Config from Apache Curator documentation. Main settings are presented below.

`zk.connectString` - default value: `localhost:2181`

`zk.connectionTimeout` - default value: `10000`

`zk.sessionTimeout` - default value: `30000`

`zk.retryPolicy.baseSleepTime` - default value: `1000`

`zk.retryPolicy.maxRetries` - default value: `5`

`zk.retryPolicy.maxSleepTime` - default value: `8000`

### Kafka settings
See Kafka Config from Apache Kafka documentation. Main settings are presented below.

`kafka.bootstrap.servers`

`kafka.replication.factor` - default value: `3`

### Cassandra settings
Settinfs for Timeline Cassandra. Configuration for Tracing Cassandra are the same but starts with prefix `tracing`.

`cassandra.nodes` - nodes of Cassandra in form `<host>[:port][,<host>[:port],...]`, default value: `127.0.0.1`,
also, default port value is `9042`

`cassandra.keyspace` - default value: `hercules`

`cassandra.replication.factor` - default value: `3`

## Command line
`java $JAVA_OPTS -jar  hercules-init.jar application.properties=file://path/to/file/application.properties init-zk=true init-kafka=true init-cassandra=true init-tracing-cassandra=true`

* init-zk - true if initialization is required in ZooKeeper (false by default);
* init-kafka - true if initialization is required in Kafka (false by default);
* init-cassandra - true if initialization is required in Cassandra (false by default);
* init-tracing-cassandra - true if initialization is required in Tracing-Cassandra (false by default).

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### `application.properties` sample:
```properties
zk.connectString=localhost:2181,localhost:2182,localhost:2183
zk.connectionTimeout=10000
zk.sessionTimeout=30000
zk.retryPolicy.baseSleepTime=1000
zk.retryPolicy.maxRetries=5
zk.retryPolicy.maxSleepTime=8000
 
kafka.bootstrap.servers=localhost:9092,localhost:9093,localhost:9094
kafka.replication.factor=3
 
cassandra.nodes=localhost
cassandra.keyspace=hercules
cassandra.replication.factor=3

tracing.cassandra.nodes=localhost
tracing.cassandra.keyspace=hercules_traces
tracing.cassandra.replication.factor=1
```
