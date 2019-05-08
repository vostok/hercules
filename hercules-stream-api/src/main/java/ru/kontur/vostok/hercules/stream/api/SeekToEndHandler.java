package ru.kontur.vostok.hercules.stream.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidDeserializer;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.Maps;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class SeekToEndHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeekToEndHandler.class);

    private static final StreamReadStateWriter CONTENT_WRITER = new StreamReadStateWriter();

    private static final String PARAM_STREAM = "stream";
    private static final String PARAM_SHARD_INDEX = "shardIndex";
    private static final String PARAM_SHARD_COUNT = "shardCount";

    private final AuthManager authManager;
    private final StreamRepository repository;
    private final String servers;

    public SeekToEndHandler(Properties properties, AuthManager authManager, StreamRepository repository) {
        this.authManager = authManager;
        this.repository = repository;
        this.servers = StreamReader.Props.SERVERS.extract(properties);
    }


    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> optionalStreamName = ExchangeUtil.extractQueryParam(exchange, PARAM_STREAM);
        if (!optionalStreamName.isPresent()) {
            ResponseUtil.badRequest(exchange, "Missing stream name");
            return;
        }

        String apiKey = optionalApiKey.get();
        String streamName = optionalStreamName.get();

        AuthResult authResult = authManager.authRead(apiKey, streamName);

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        Optional<Integer> optionalShardIndex = ExchangeUtil.extractIntegerQueryParam(exchange, PARAM_SHARD_INDEX);
        if (!optionalShardIndex.isPresent() || optionalShardIndex.get() < 0) {
            ResponseUtil.badRequest(exchange, "Missing or invalid " + PARAM_SHARD_INDEX);
            return;
        }

        Optional<Integer> optionalShardCount = ExchangeUtil.extractIntegerQueryParam(exchange, PARAM_SHARD_COUNT);
        if (!optionalShardCount.isPresent() || optionalShardCount.get() < 1) {
            ResponseUtil.badRequest(exchange, "Missing or invalid " + PARAM_SHARD_COUNT);
            return;
        }

        Optional<Stream> stream;
        try {
            stream = repository.read(streamName);
        } catch (Exception ex) {
            LOGGER.error("Cannot read stream due to exception", ex);
            ResponseUtil.internalServerError(exchange);
            return;
        }
        if (!stream.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }

        try {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, VoidDeserializer.class);

            KafkaConsumer<Void, byte[]> consumer = new KafkaConsumer<>(props);

            List<TopicPartition> partitions = Arrays.stream(
                    LogicalPartitioner.getPartitionsForLogicalSharding(
                            stream.get(),
                            optionalShardIndex.get(),
                            optionalShardCount.get())).
                    mapToObj(partition -> new TopicPartition(streamName, partition)).
                    collect(Collectors.toList());
            consumer.seekToEnd(partitions);

            Map<TopicPartition, Long> map = new HashMap<>(Maps.effectiveHashMapCapacity(partitions.size()));
            for (TopicPartition partition : partitions) {
                map.put(partition, consumer.position(partition));
            }

            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MimeTypes.APPLICATION_OCTET_STREAM);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = new Encoder(outputStream);
            CONTENT_WRITER.write(encoder, StreamReadStateUtil.stateFromMap(streamName, map));

            exchange.getResponseSender().send(ByteBuffer.wrap(outputStream.toByteArray()));
        } catch (Exception ex) {
            LOGGER.error("Error on processing request", ex);
            ResponseUtil.internalServerError(exchange);
        }
    }
}
