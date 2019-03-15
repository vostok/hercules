# Hercules Tracing Sink
Tracing Sink is used to move traces from Kafka to Apache Cassandra.

## Command line
`java $JAVA_OPTS -jar hercules-tracing-sink.jar application.properties=file:///path/to/properties/file`

### Initialization
Stream of trace spans should be predefined.
