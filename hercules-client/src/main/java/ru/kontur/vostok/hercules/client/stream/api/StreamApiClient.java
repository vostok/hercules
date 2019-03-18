package ru.kontur.vostok.hercules.client.stream.api;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import ru.kontur.vostok.hercules.client.CommonHeaders;
import ru.kontur.vostok.hercules.client.CommonParameters;
import ru.kontur.vostok.hercules.client.LogicalShardState;
import ru.kontur.vostok.hercules.client.exceptions.BadRequestException;
import ru.kontur.vostok.hercules.client.exceptions.ForbiddenException;
import ru.kontur.vostok.hercules.client.exceptions.HerculesClientException;
import ru.kontur.vostok.hercules.client.exceptions.HerculesClientExceptionUtil;
import ru.kontur.vostok.hercules.client.exceptions.NotFoundException;
import ru.kontur.vostok.hercules.client.exceptions.UnauthorizedException;
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
import java.util.Optional;
import java.util.function.Supplier;

/**
 * StreamApiClient - client for Hercules Stream API
 *
 * @author Kirill Sulim
 */
public class StreamApiClient {

    private static final StreamReadStateWriter STATE_WRITER = new StreamReadStateWriter();
    private static final EventStreamContentReader CONTENT_READER = new EventStreamContentReader();

    private final CloseableHttpClient httpClient;
    private final URI server;
    private final LogicalShardState shardState;
    private final String apiKey;

    /**
     * @param server     server URI
     * @param shardState client logical shard state
     * @param apiKey     api key for authorization
     */
    public StreamApiClient(
            URI server,
            LogicalShardState shardState,
            String apiKey
    ) {
        this.httpClient = HttpClients.createDefault();
        this.server = server;
        this.shardState = shardState;
        this.apiKey = apiKey;
    }

    /**
     * @param httpClient apache http client instance
     * @param server     server URI
     * @param shardState client logical shard state
     * @param apiKey     api key for authorization
     */
    public StreamApiClient(
            CloseableHttpClient httpClient,
            URI server,
            LogicalShardState shardState,
            String apiKey
    ) {
        this.httpClient = httpClient;
        this.server = server;
        this.shardState = shardState;
        this.apiKey = apiKey;
    }

    /**
     * @param httpClientFactory apache http client supplier
     * @param server            server URI
     * @param shardState        client logical shard state
     * @param apiKey            api key for authorization
     */
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

    /**
     * Get stream content from Stream API
     *
     * @param stream          stream name
     * @param streamReadState stream read state
     * @param count           event count
     * @return stream content
     * @throws HerculesClientException in case of unspecified error
     * @throws BadRequestException     in case of incorrect parameters
     * @throws UnauthorizedException   in case of missing authorization data
     * @throws ForbiddenException      in case of request of forbidden resource
     * @throws NotFoundException       in case of not found resource
     */
    public EventStreamContent getStreamContent(
            final String stream,
            final StreamReadState streamReadState,
            final int count
    ) throws HerculesClientException,
            BadRequestException,
            UnauthorizedException,
            ForbiddenException,
            NotFoundException {
        URI uri = ThrowableUtil.toUnchecked(() -> new URIBuilder(server.resolve(Resources.STREAM_READ))
                .addParameter(Parameters.STREAM, stream)
                .addParameter(Parameters.RESPONSE_EVENTS_COUNT, String.valueOf(count))
                .addParameter(CommonParameters.LOGICAL_SHARD_ID, String.valueOf(shardState.getShardId()))
                .addParameter(CommonParameters.LOGICAL_SHARD_COUNT, String.valueOf(shardState.getShardCount()))
                .build());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(calculateReadStateSize(streamReadState.getShardCount()));
        STATE_WRITER.write(new Encoder(bytes), streamReadState);

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader(CommonHeaders.API_KEY, apiKey);
        httpPost.setEntity(new ByteArrayEntity(bytes.toByteArray()));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            final Optional<HerculesClientException> exception = HerculesClientExceptionUtil.exceptionFromStatus(
                    response.getStatusLine().getStatusCode(),
                    stream,
                    apiKey
            );

            if (exception.isPresent()) {
                throw exception.get();
            }

            HttpEntity entity = response.getEntity();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) entity.getContentLength());
            entity.writeTo(outputStream);
            return CONTENT_READER.read(new Decoder(outputStream.toByteArray()));
        } catch (IOException e) {
            throw new HerculesClientException("IOException occurred", e);
        }
    }

    /**
     * @return true if ping was performed without errors
     */
    public boolean ping() {
        HttpGet httpGet = new HttpGet(server.resolve(Resources.PING));

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return 200 == response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Return read state size in bytes                                          <br>
     * <code>
     * |......Read State.......|                                                <br>
     * |.Count.|.Shard State *.|                                                <br>
     * </code>
     * Where, Count is Integer.
     * <p>
     * <code>
     * |....Shard State.....|                                                   <br>
     * |.Partition.|.Offset.|                                                   <br>
     * </code>
     * Where, Partition is Integer, Offset is Long.
     * <p>
     * Thus,                                                                    <br>
     * sizeof(ReadState) = sizeof(Count) + Count * sizeof(ShardState)           <br>
     * sizeof(ShardState) = sizeof(Partition) + sizeof(Offset)
     *
     * @param shardCount shard count
     * @return read state size in bytes
     */
    private static int calculateReadStateSize(int shardCount) {
        return SizeOf.INTEGER + shardCount * (SizeOf.INTEGER + SizeOf.LONG);
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

    private static class Parameters {
        /**
         * Stream pattern
         */
        static final String STREAM = "stream";

        /**
         * Event count
         */
        static final String RESPONSE_EVENTS_COUNT = "take";
    }
}
