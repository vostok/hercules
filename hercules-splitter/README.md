# Hercules Splitter
Hercules Splitter is used to split one stream into several by using hash. For example, for distribute write operation in sharded storage nodes. 
Nodes in this application means nodes of destination storage.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Kafka Streams settings
`kafka.streams` - Scope for Kafka Streams properties.

Required parameters are:

`kafka.streams.application.id` - The application ID. Each stream processing application must have a unique ID. The same ID must be given to all instances of the application.
For example, if you're using splitter for sharding write operation between nodes of Graphite, you can set for all instances of splitter involved in this 
operation application.id value "graphite-splitter".

`kafka.streams.bootstrap.servers` - List of Kafka's cluster servers. 

Other parameter you can find in Kafka's [documentation](https://kafka.apache.org/10/documentation/streams/developer-guide/config-streams.html).

### Application context settings
`context.instance.id` - Id of instance.

`context.environment` - Id of environment.

`context.zone` - Id of zone.

### Specific settings

`splitter.shardingKeys` - List of first level tags that will be used as hash function arguments. 

`splitter.hashAlgorithm` - Hash function implementation name that will be used. Possible algorithm names:
* xxhash32_fastest_instance;
* xxhash32_native_instance;
* xxhash32_safe_instance;
* xxhash32_unsafe_instance;
* xxhash32_fastest_java_instance;
* adler32;
* murmur3_32;
* murmur3_128;
* crc32;
* crc32c;
* sha256;
* sha384;
* sha512;
* sip24;
* md5;
* sha1.

`splitter.pattern` - Pattern of streams are subscribed by consumers.

`splitter.pattern.exclusions` - Pattern of streams which should be excluded from processing (optional).

`splitter.outputStreamTemplate` - Pattern of output streams. Placeholder `<node>` will be replaced by node name from the `splitter.nodeWeights` parameter. 

`splitter.nodeWeights` - Scope for node weights. Weights can be real numbers but only positive.
Node names should contain only valid characters of stream name. Example:
```properties
splitter.nodeWeights.node1=1
splitter.nodeWeights.node2=1.5
```

## `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6080

context.environment=production
context.zone=default
context.instance.id=1

metrics.graphite.server.addr=graphite.example.org
metrics.graphite.server.port=2003
metrics.graphite.prefix=vostok.hercules
metrics.period=60

splitter.shardingKeys=tags
splitter.hashAlgorithm=xxhash32_fastest_java_instance
splitter.pattern=input_stream
splitter.pattern.exclusions=
splitter.outputStreamTemplate=output_stream_<node>
splitter.nodeWeights.node1=1.0
splitter.nodeWeights.node2=1.0

kafka.streams.application.id=metrics-splitter
kafka.streams.bootstrap.servers=01.kafka.example.org:9092,02.kafka.example.org:9092,03.kafka.example.org:9092,
kafka.streams.producer.compression.type=gzip
kafka.streams.metrics.recording.level=DEBUG
```
