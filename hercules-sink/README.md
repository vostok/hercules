# Hercules Sink

This module does not provide any runnable application.
Module contains base Sink abstractions to build another sinks.

## Common settings

`sink.pattern` - pattern of streams are subscribed by consumers, required

`sink.pattern.exclusions` - pattern of streams which should be excluded from processing, optional

`sink.groupId` - a unique string that identifies the consumer group this consumer belongs to

`sink.batchSize` - size of batch with Log Events, default value: `1000`

`sink.pollTimeoutMs` - poll duration when read from Apache Kafka, default value: `6000`

`sink.availabilityTimeoutMs` - maximum time to wait for processor's availability

## AbstractSinkParallelDaemon settings

`sink.sinkType` - select implementations: BASE for BaseSinkDaemonStarter and PARALLEL for ParallelSinkDaemonStarter

## BaseSinkDaemonStarter settings

`sink.poolSize` - number of threads are reading from Apache Kafka, default value: `1`

## ParallelSinkDaemonStarter settings

`sink.strategy.batchByteSize` - maximum byte size of one batch

`sink.offsetsQueueSize` - maximum commit queue size

### ParallelEventConsumer settings

`sink.pollingDelayMs` - delay between poll if queues are full or events received are less than the batch size

`sink.maxEventsCount` - maximum number of reading events

`sink.maxEventsByteSize` - maximum byte size of reading events

### ParallelSenderStrategy settings

`sink.strategy.preparePoolSize` - number of processing threads

`sink.strategy.sendPoolSize` - number of sending threads

`sink.strategy.maxAllBatchesByteSize` - maximum byte size of all processed and sent batches

`sink.strategy.waitBatchesPeriodMs` - time that the strategy will wait if there is no data or no changing process
statuses

`sink.strategy.fakeSenderDelay` - if set, instead of sending data, the thread will sleep for a random time between the
set time and twice

### CreateEventsBatchStrategy settings

`sink.strategy.batchSize` - maximum number of sent messages in one batch - optional parameter, if not set sink.batchSize
is used

`sink.strategy.maxPartitionsInBatch` - maximum number of partitions in one batch

`sink.strategy.createBatchTimeoutMs` - maximum waiting time before creating a batch, if there are fewer events in the
queue for a specific TopicPartition than batchSize

## Kafka consumer settings

All Kafka consumer settings have `sink.consumer` prefix.
See the documentation of a consumer for available settings.
There are the most important settings listed below.

`sink.consumer.bootstrap.servers` - list of Apache Kafka hosts

`sink.consumer.max.partition.fetch.bytes` - max batch size for reading from one partition

`sink.consumer.max.poll.interval.ms` - time, after which Apache Kafka will exclude the consumer from group if it doesn't
poll or commit

`sink.consumer.metric.reporters` - a list of classes to use as metrics reporters

