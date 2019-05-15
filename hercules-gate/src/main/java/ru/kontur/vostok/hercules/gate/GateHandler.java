package ru.kontur.vostok.hercules.gate;

import com.codahale.metrics.Meter;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.throttling.Throttle;
import ru.kontur.vostok.hercules.util.Maps;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class GateHandler implements HttpHandler {
    private final MetricsCollector metricsCollector;

    private final AuthManager authManager;
    private final Throttle<HttpServerRequest, SendContext> throttle;
    private final StreamStorage streamStorage;
    private final AuthValidationManager authValidationManager;

    private final boolean async;
    private final long maxContentLength;

    private final Meter requestMeter;
    private final Meter requestSizeMeter;

    public GateHandler(
            MetricsCollector metricsCollector,
            AuthManager authManager,
            Throttle<HttpServerRequest, SendContext> throttle,
            AuthValidationManager authValidationManager,
            StreamStorage streamStorage,
            boolean async,
            long maxContentLength
    ) {
        this.metricsCollector = metricsCollector;

        this.authManager = authManager;
        this.throttle = throttle;
        this.authValidationManager = authValidationManager;
        this.streamStorage = streamStorage;

        this.async = async;
        this.maxContentLength = maxContentLength;

        if (async) {
            this.requestMeter  = metricsCollector.meter("gateHandlerAsyncRequests");
            this.requestSizeMeter = metricsCollector.meter("gateHandlerAsyncRequestSizeBytes");
        }
        else {
            this.requestMeter = metricsCollector.meter("gateHandlerSyncRequests");
            this.requestSizeMeter  = metricsCollector.meter("gateHandlerSyncRequestSizeBytes");
        }
    }

    @Override
    public void handle(HttpServerRequest request) {
        requestMeter.mark(1);

        String stream = request.getParameter("stream");
        if (stream == null) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }
        //TODO: stream name validation

        String apiKey = request.getHeader("apiKey");
        if (apiKey == null) {
            request.complete(HttpStatusCodes.UNAUTHORIZED);
            return;
        }
        if (!auth(request, apiKey, stream)) {
            return;
        }

        // Check content length
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }
        int contentLength = optionalContentLength.get();
        if (contentLength < 0) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }
        if (contentLength > maxContentLength) {
            request.complete(HttpStatusCodes.REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        requestSizeMeter.mark(contentLength);

        Optional<Stream> optionalBaseStream = streamStorage.read(stream);
        if (!optionalBaseStream.isPresent()) {
            request.complete(HttpStatusCodes.NOT_FOUND);
            return;
        }
        Stream baseStream = optionalBaseStream.get();
        if (!(baseStream instanceof BaseStream)) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }

        Set<String> tagsToValidate = authValidationManager.getTags(apiKey, stream);

        ShardingKey shardingKey = ShardingKey.fromKeyPaths(baseStream.getShardingKey());
        int partitions = baseStream.getPartitions();
        String topic = baseStream.getName();

        Set<String> tags = new HashSet<>(Maps.effectiveHashMapCapacity(shardingKey.size() + tagsToValidate.size()));
        Arrays.stream(shardingKey.getKeys()).map(HPath::getRootTag).forEach(tags::add);//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        tags.addAll(tagsToValidate);

        ContentValidator validator = authValidationManager.validator(apiKey, stream);

        SendContext context = new SendContext(async, topic, tags, partitions, shardingKey, validator);
        throttle.throttleAsync(request, context);
    }

    private boolean auth(HttpServerRequest request, String apiKey, String stream) {
        AuthResult authResult = authManager.authWrite(apiKey, stream);

        if (authResult.isSuccess()) {
            return true;
        }

        if (authResult.isUnknown()) {
            request.complete(HttpStatusCodes.UNAUTHORIZED);
            return false;
        }

        request.complete(HttpStatusCodes.FORBIDDEN);
        return false;
    }
}
