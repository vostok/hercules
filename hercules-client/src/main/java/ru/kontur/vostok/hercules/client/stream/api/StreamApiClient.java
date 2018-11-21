package ru.kontur.vostok.hercules.client.stream.api;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import ru.kontur.vostok.hercules.client.LogicalShardState;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventStreamContentReader;
import ru.kontur.vostok.hercules.protocol.decoder.SizeOf;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.StreamReadStateWriter;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

/**
 * StreamApiClient
 *
 * @author Kirill Sulim
 */
public class StreamApiClient {

    private static final StreamReadStateWriter STATE_WRITER = new StreamReadStateWriter();
    private static final EventStreamContentReader CONTENT_READER = new EventStreamContentReader();

    private final URI server;
    private final LogicalShardState shardState;
    private final String apiKey;
    private final CloseableHttpClient httpClient;

    public StreamApiClient(
            Supplier<CloseableHttpClient> httpClientFactory,
            URI server,
            LogicalShardState shardState,
            String apiKey
    ) {
        this.httpClient = httpClientFactory.get();
        this.server = server;
        this.shardState = shardState;
        this.apiKey = apiKey;
    }

    public EventStreamContent getStreamContent(String pattern, StreamReadState streamReadState, int count) {

        URI uri = ThrowableUtil.toUnchecked(() -> new URIBuilder(server.resolve(Resources.STREAM_READ))
                .addParameter(Parameters.STREAM_PATTERN, pattern)
                .addParameter(Parameters.RESPONSE_EVENTS_COUNT, String.valueOf(count))
                .addParameter(Parameters.LOGICAL_SHARD_ID, String.valueOf(shardState.getShardId()))
                .addParameter(Parameters.LOGICAL_SHARD_COUNT, String.valueOf(shardState.getShardCount()))
                .build());

        ByteArrayOutputStream stream = new ByteArrayOutputStream(calculateReadStateSize(streamReadState.getShardCount()));
        STATE_WRITER.write(new Encoder(stream), streamReadState);

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader(Headers.API_KEY, apiKey);
        httpPost.setEntity(new ByteArrayEntity(stream.toByteArray()));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) entity.getContentLength());
            entity.writeTo(outputStream);
            return CONTENT_READER.read(new Decoder(outputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean ping() {

        HttpGet httpGet = new HttpGet(server.resolve(Resources.PING));

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return 200 == response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            return false;
        }
    }

    private static int calculateReadStateSize(int shardCount) {
        return SizeOf.VECTOR_LENGTH + shardCount * (SizeOf.INTEGER + SizeOf.LONG);
    }

    private static class Resources {
        /**
         * Get stream content
         */
        static final URI STREAM_READ = URI.create("./stream/read");

        /**
         * Ping stream API
         */
        static final URI PING = URI.create("./ping");
    }

    private static class Headers {
        /**
         * Authorization key
         */
        static final String API_KEY = "apiKey";
    }

    private static class Parameters {
        /**
         * Stream pattern
         */
        static final String STREAM_PATTERN = "stream";

        /**
         * Logical shard id
         */
        static final String LOGICAL_SHARD_ID = "k";

        /**
         * Logical shard count
         */
        static final String LOGICAL_SHARD_COUNT = "n";

        /**
         * Event count
         */
        static final String RESPONSE_EVENTS_COUNT = "take";
    }
}
