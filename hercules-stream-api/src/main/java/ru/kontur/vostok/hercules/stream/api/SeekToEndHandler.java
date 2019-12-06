package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;
import ru.kontur.vostok.hercules.util.ByteBufferPool;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class SeekToEndHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeekToEndHandler.class);

    private static final StreamReadStateWriter CONTENT_WRITER = new StreamReadStateWriter();

    private final AuthProvider authProvider;
    private final StreamRepository streamRepository;
    private final ConsumerPool<Void, byte[]> consumerPool;

    public SeekToEndHandler(AuthProvider authProvider, StreamRepository repository, ConsumerPool<Void, byte[]> consumerPool) {
        this.authProvider = authProvider;
        this.streamRepository = repository;
        this.consumerPool = consumerPool;
    }


    @Override
    public void handle(HttpServerRequest request) {
        ParameterValue<String> streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (!streamName.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.STREAM.name() + " error: " + streamName.result().error());
            return;
        }

        AuthResult authResult = authProvider.authRead(request, streamName.get());
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        ParameterValue<Integer> shardIndex = QueryUtil.get(QueryParameters.SHARD_INDEX, request);
        if (!shardIndex.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.SHARD_INDEX.name() + " error: " + shardIndex.result().error());
            return;
        }

        ParameterValue<Integer> shardCount = QueryUtil.get(QueryParameters.SHARD_COUNT, request);
        if (!shardCount.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.SHARD_COUNT.name() + " error: " + shardCount.result().error());
            return;
        }

        if (shardCount.get() <= shardIndex.get()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Invalid parameters: " + QueryParameters.SHARD_COUNT.name() + " must be > " + QueryParameters.SHARD_INDEX.name());
            return;
        }

        Stream stream;
        try {
            Optional<Stream> optionalStream = streamRepository.read(streamName.get());
            if (!optionalStream.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            stream = optionalStream.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        Consumer<Void, byte[]> consumer = null;
        try {
            consumer = consumerPool.acquire(5_000, TimeUnit.MILLISECONDS);

            List<TopicPartition> partitions = Arrays.stream(
                    LogicalPartitioner.getPartitionsForLogicalSharding(
                            stream,
                            shardIndex.get(),
                            shardCount.get())).
                    mapToObj(partition -> new TopicPartition(stream.getName(), partition)).
                    collect(Collectors.toList());
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            StreamReadState streamReadState = StreamReadStateUtil.stateFromMap(stream.getName(), endOffsets);

            request.getResponse().setContentType(MimeTypes.APPLICATION_OCTET_STREAM);

            ByteBuffer buffer = ByteBufferPool.acquire(streamReadState.sizeOf());
            Encoder encoder = new Encoder(buffer);
            CONTENT_WRITER.write(encoder, streamReadState);
            buffer.flip();
            request.getResponse().setContentLength(buffer.remaining());
            request.getResponse().send(
                    buffer,
                    req -> {
                        request.complete();
                        ByteBufferPool.release(buffer);
                    },
                    (req, exception) -> {
                        LOGGER.error("Error when send response", exception);
                        request.complete();
                        ByteBufferPool.release(buffer);
                    });
        } catch (Exception ex) {
            LOGGER.error("Error on processing request", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        } finally {
            if (consumer != null) {
                consumerPool.release(consumer);
            }
        }
    }
}
