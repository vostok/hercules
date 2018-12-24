package ru.kontur.vostok.hercules.client.timeline.api;

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
import ru.kontur.vostok.hercules.protocol.TimelineContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.SizeOf;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineContentReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineReadStateWriter;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * TimelineApiClient - client for Hercules Timeline API
 *
 * @author Kirill Sulim
 */
public class TimelineApiClient {

    private static final TimelineReadStateWriter STATE_WRITER = new TimelineReadStateWriter();
    private static final TimelineContentReader CONTENT_READER = new TimelineContentReader(EventReader.readAllTags());

    private final CloseableHttpClient httpClient;
    private final URI server;
    private final LogicalShardState shardState;
    private final String apiKey;

    /**
     * @param server     server URI
     * @param shardState client logical shard state
     * @param apiKey     api key for authorization
     */
    public TimelineApiClient(
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
    public TimelineApiClient(
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
    public TimelineApiClient(
            final Supplier<CloseableHttpClient> httpClientFactory,
            final URI server,
            final LogicalShardState shardState,
            final String apiKey
    ) {
        this.httpClient = httpClientFactory.get();
        this.server = server;
        this.shardState = shardState;
        this.apiKey = apiKey;
    }

    /**
     * Request timeline content from timeline API
     *
     * @param timeline          timeline name
     * @param timelineReadState read state
     * @param timeInterval      time interval
     * @param count             count of events
     * @return timeline content
     * @throws HerculesClientException in case of unspecified error
     * @throws BadRequestException     in case of incorrect parameters
     * @throws UnauthorizedException   in case of missing authorization data
     * @throws ForbiddenException      in case of request of forbidden resource
     * @throws NotFoundException       in case of not found resource
     */
    public TimelineContent getTimelineContent(
            final String timeline,
            final TimelineReadState timelineReadState,
            final TimeInterval timeInterval,
            final int count
    ) throws HerculesClientException,
            BadRequestException,
            UnauthorizedException,
            ForbiddenException,
            NotFoundException {

        URI uri = ThrowableUtil.toUnchecked(() -> new URIBuilder(server.resolve(Resources.TIMELINE_READ))
                .addParameter(Parameters.TIMELINE, timeline)
                .addParameter(Parameters.RESPONSE_EVENTS_COUNT, String.valueOf(count))
                .addParameter(CommonParameters.LOGICAL_SHARD_ID, String.valueOf(shardState.getShardId()))
                .addParameter(CommonParameters.LOGICAL_SHARD_COUNT, String.valueOf(shardState.getShardCount()))
                .addParameter(Parameters.LEFT_TIME_BOUND, String.valueOf(timeInterval.getFrom()))
                .addParameter(Parameters.RIGHT_TIME_BOUND, String.valueOf(timeInterval.getTo()))
                .build());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(calculateReadStateSize(timelineReadState.getShardCount()));
        STATE_WRITER.write(new Encoder(bytes), timelineReadState);

        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader(CommonHeaders.API_KEY, apiKey);
        httpPost.setEntity(new ByteArrayEntity(bytes.toByteArray()));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            final Optional<HerculesClientException> exception = HerculesClientExceptionUtil.exceptionFromStatus(
                    response.getStatusLine().getStatusCode(),
                    timeline,
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
            throw new HerculesClientException("IO Exception occurred", e);
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
     * Return read state size in bytes                                              <br>
     * <code>
     * |........Timeline State..........|                                           <br>
     * |.Count.|.Timeline Slice State *.|                                           <br>
     * </code>
     * Where, Count is Integer.
     * <p>
     * <code>
     * |...Timeline Slice State...|                                                 <br>
     * |.Slice.|.Offset.|.EventId.|                                                 <br>
     * </code>
     * Where, Slice is Integer, Offset is Long.
     * <p>
     * <code>
     * |.....EventId......|
     * |.Timestamp.|.Uuid.|
     * </code>
     * Where, Timestamp is Long, Uuid is UUID.
     * <p>
     * Thus,                                                                        <br>
     * sizeof(TimelineState) = sizeof(Count) + Count * sizeof(TimelineSliceState)   <br>
     * sizeof(TimelineSliceState) = sizeof(Slice) + sizeof(Offset) + sizeof(EventId)<br>
     * sizeof(EventId) = sizeof(Timestamp) + sizeof(Uuid)
     *
     * @param shardCount shard count
     * @return read state size in bytes
     */
    private static int calculateReadStateSize(int shardCount) {
        return SizeOf.INTEGER + shardCount * (SizeOf.INTEGER + SizeOf.LONG + (SizeOf.LONG + SizeOf.UUID));
    }

    private static class Resources {
        /**
         * Get stream content
         */
        static final URI TIMELINE_READ = URI.create("./timeline/read");

        /**
         * Ping stream API
         */
        static final URI PING = URI.create("./ping");
    }

    private static class Parameters {
        /**
         * Timeline
         */
        static final String TIMELINE = "timeline";

        /**
         * Event count
         */
        static final String RESPONSE_EVENTS_COUNT = "take";

        /**
         * Left inclusive time bound
         */
        static final String LEFT_TIME_BOUND = "from";

        /**
         * Right exclusive time bound
         */
        static final String RIGHT_TIME_BOUND = "to";
    }
}
