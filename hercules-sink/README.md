# Hercules Sink
This module does not provide any runnable application.
Module contains base Sink abstractions to build another sinks.

## Sink settings
`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.batchSize` - size of batch with Log Events, default value: `1000`

`sink.pattern` - pattern of streams are subscribed by consumers, required

`sink.pattern.exclusions` - pattern of streams which should be excluded from processing, optional

### Kafka consumer settings
All Kafka consumer settings have `sink.consumer` prefix.
See the documentation of a consumer for available settings.
There are the most important settings listed below.

`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

