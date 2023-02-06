# Hercules Radamant
Hercules Radamant is used to select events that satisfied one or more rules,
enrich and send events to the output stream or streams.

## Settings
Application is configured through properties file.

### Main Application settings
`application.host` - server host, default value: `0.0.0.0`

`application.port` - server port, default value: `8080`

### Application context settings
`context.environment` - Id of environment.

`context.zone` - Id of zone.

`context.instance.id` - Id of instance.

### Kafka Streams settings
`kafka.streams` - Scope for Kafka Streams properties.

Required parameters are:

`kafka.streams.application.id` - The application ID. Each stream processing application must have a unique ID. The same ID must be given to all instances of the application.
For example, if you're using splitter for sharding write operation between nodes of Graphite, you can set for all instances of splitter involved in this 
operation application.id value "graphite-splitter".

`kafka.streams.bootstrap.servers` - List of Kafka's cluster servers. 

Other parameter you can find in Kafka's [documentation](https://kafka.apache.org/10/documentation/streams/developer-guide/config-streams.html).

### Specific settings
`radamant.pattern` - Pattern of streams are subscribed by consumers.

`radamant.default.result.stream` - Default value of output stream. 

`radamant.config.path` - Path to file with rules, default value: `file://rules_flat.json`

## `application.properties` sample:
```
application.host=0.0.0.0
application.port=6512

context.environment=dev
context.zone=default
context.instance.id=single

kafka.streams.application.id=radamant
kafka.streams.bootstrap.servers=
kafka.streams.producer.compression.type=gzip
kafka.streams.metrics.recording.level=DEBUG

metrics.graphite.server.addr=localhost
metrics.graphite.server.port=2003
metrics.graphite.prefix=vostok.hercules
metrics.period=60

radamant.pattern=relay_metrics
radamant.default.result.stream=metrics_for_aggregation
radamant.config.path=file://rules_flat.json
```
## Rules configuration
You can configure rules for flat metrics through *.json file.

### Format of rule:
```
`rule_name` - Name of rule. Cannot contains spaces. Required field. 
`description` - Human readable description for rule, optional field.
`pattern` - Regular expression for metrics name. Required field.
`enrichment` - Set of key-value pairs. Will be added to event. Required field.
`name_pattern` - Regular expression for new metrics name. Required field.
```

## `rules_flat.json` sample:
```
[
  {
    "rule_name": "test_rule",
    "description": "Rule description",
    "pattern": "pattern",
    "enrichment": {
      "name_pattern": "new.metric.name.{1}",
      "period": "60",
      "expire after": "90",
      "timestamp": "start of bucket",
      "function": "sum"
    }
  }
]
```
