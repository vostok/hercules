# Hercules Tracing Sink
Tracing Sink is used to move traces from Kafka to Apache Cassandra.

## Command line
`java $JAVA_OPTS -jar hercules-tracing-sink.jar application.properties=file:///path/to/properties/file`

### Initialization
Tracing Sink uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.
