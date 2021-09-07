# Hercules Graphite Adapter
Graphite Adapter is a Graphite-compatible TCP-server.
Graphite Adapter receives metrics in the plaintext format and sends as Hercules events to the [Gate](../hercules-gate/README.md).

Graphite Adapter supports metrics with or without tags in the format is as follows:
```
[{metric-prefix}.]{metric-name}[;{tag-key}={tag-value}]* {value} {timestamp}
```
Here,
- `{metric-prefix}` is an optional dot-separated hierarchical prefix.
- `{metric-name`} is a metric name
- `{tag-key}={tag-value}` is an optional tag. A semicolon separates tags from each other.

See Graphite docs for details:
- [The plaintext protocol](https://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-plaintext-protocol).
- [Tag support in Graphite](https://graphite.readthedocs.io/en/latest/tags.html?highlight=tags#carbon).

Graphite Adapter validates metrics. A valid metric satisfies the following conditions:
- a metric value isn't a `NaN`
- a metric name is an ASCII string contains the latin characters, decimal digits and an underscore `_`.
- a metric prefix additionally may contain dots `.`
- a timestamp is a valid Unix time.

Also, Graphite Adapter supports ACL (access-control list) to filter metrics by the prefix and name.
See examples below.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - HTTP-server host, default value: `0.0.0.0`

`application.port` - HTTP-server port, default value: `8080`

### Metrics processing settings
`purgatory.plain.metrics.stream` - the stream for metrics without tags, required

`purgatory.tagged.metrics.stream` - the stream for metrics with tags, required

`purgatory.filter.acl.file.path` - path to file with ACL, required

`purgatory.filter.acl.default.rule` - default ACL rule, default value: `PERMIT`

`accumulator.plain.metrics.stream` - workaround property due to legacy event publisher, must be set to the value of `purgatory.plain.metrics.stream`

`accumulator.tagged.metrics.stream` workaround property due to legacy event publisher, must be set to the value of `purgatory.tagged.metrics.stream`

### TCP server settings
`server.host` - TCP-server host, default value: `0.0.0.0`

`server.port` - TCP-server port, default value: `2003`

`server.worker.thread.count` - worker thread count, default value `CPU cores * 2`

`server.read.timeout.ms` - wait new metrics for this period in millis, default value `90000`

`server.recv.buffer.size.bytes` - recv buffer size in bytes, value for socket option `SO_RCVBUF`, use system default if not set

### Graphite metrics reporter settings
`metrics.graphite.server.addr` - hostname of graphite instance, default value: `localhost`

`metrics.graphite.server.port` - port of graphite instance, default value: `2003`

`metrics.graphite.prefix` - prefix is added to metric name

`metrics.period` - the period to send metrics to graphite, default value: `60`

### Application context settings
`context.instance.id` - id of instance

`context.environment` - deployment environment (production, staging and so on)

`context.zone` - id of zone

### Http Server settings
`http.server.ioThreads` - the number of IO threads. Default value: `1`.

`http.server.workerThreads` - the number of worker threads. Default value: `1`.

## Command line
```shell script
java $JAVA_OPTS \
     -Dhercules.gate.client.config=/path/to/file/graphite-adapter.gate-client.properties \
     -jar hercules-graphite-adapter.jar \
     application.properties=file://path/to/file/application.properties
```

Also, ZooKeeper can be used as source of `application.properties` file:  
```
zk://zk_host_1:port[,zk_host_2:port,...]/path/to/znode/application.properties
```

## Quick start
### Initialization
Streams for metric events should be predefined.

### `application.properties` sample:
```properties
application.host=0.0.0.0
application.port=6402

purgatory.plain.metrics.stream=metrics_dev_plain
purgatory.tagged.metrics.stream=metrics_dev_tagged
purgatory.filter.acl.file.path=file://graphite-adapter.acl
purgatory.filter.acl.default.rule=PERMIT
accumulator.plain.metrics.stream=metrics_dev_plain
accumulator.tagged.metrics.stream=metrics_dev_tagged

server.host=0.0.0.0
server.port=2003
server.worker.thread.count=4
server.read.timeout.ms=90000
server.recv.buffer.size.bytes=65536

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=hercules
metrics.period=60

context.instance.id=1
context.environment=dev
context.zone=default

http.server.ioThreads=1
http.server.workerThreads=1
```

### `graphite-adapter.acl` sample
```text
# Permits metric names starting with 'test.hercules.'
PERMIT test\.hercules\..*

# Deny other metric names starting with 'test.'
DENY test\..*

# Permit or deny all other metrics depending on default rule
```

### `graphite-adapter.gate-client.properties`
```properties
urls=http://localhost:6306
apiKey=api_key

project=hercules
environment=dev

```