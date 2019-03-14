# Hercules Graphite Sink
Graphite Sink is used to move metrics from Kafka to Graphite.

## Command line
`java $JAVA_OPTS -jar hercules-graphite-sink.jar application.properties=file://path/to/properties/file`

### Initialization
Graphite Sink uses Stream's metadata and auth rules from ZooKeeper. Thus, ZK should be configured by [Hercules Init](../hercules-init/README.md). See Hercules Init for details.
