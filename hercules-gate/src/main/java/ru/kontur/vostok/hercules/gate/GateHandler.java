package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.throttling.CapacityThrottle;
import ru.kontur.vostok.hercules.throttling.ThrottleResult;
import ru.kontur.vostok.hercules.throttling.ThrottledBy;
import ru.kontur.vostok.hercules.throttling.ThrottledRequestProcessor;
import ru.kontur.vostok.hercules.util.Maps;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GateHandler implements HttpHandler {
    private final AuthProvider authProvider;
    private final CapacityThrottle<HttpServerRequest> throttle;
    private final ThrottledRequestProcessor<HttpServerRequest> throttledRequestProcessor;
    private final SendRequestProcessor sendRequestProcessor;
    private final StreamStorage streamStorage;
    private final AuthValidationManager authValidationManager;

    private final boolean async;
    private final long maxContentLength;

    private final TimeSource time;

    private final Timer requestThrottleDurationMsTimer;

    public GateHandler(
            AuthProvider authProvider,
            CapacityThrottle<HttpServerRequest> throttle,
            ThrottledRequestProcessor<HttpServerRequest> throttledRequestProcessor,
            SendRequestProcessor sendRequestProcessor,
            AuthValidationManager authValidationManager,
            StreamStorage streamStorage,
            boolean async,
            long maxContentLength,
            MetricsCollector metricsCollector) {
        this(
                authProvider,
                throttle,
                throttledRequestProcessor,
                sendRequestProcessor,
                authValidationManager,
                streamStorage,
                async,
                maxContentLength,
                metricsCollector,
                TimeSource.SYSTEM);
    }

    GateHandler(
            AuthProvider authProvider,
            CapacityThrottle<HttpServerRequest> throttle,
            ThrottledRequestProcessor<HttpServerRequest> throttledRequestProcessor,
            SendRequestProcessor sendRequestProcessor,
            AuthValidationManager authValidationManager,
            StreamStorage streamStorage,
            boolean async,
            long maxContentLength,
            MetricsCollector metricsCollector,
            TimeSource time) {
        this.authProvider = authProvider;
        this.throttle = throttle;
        this.throttledRequestProcessor = throttledRequestProcessor;
        this.sendRequestProcessor = sendRequestProcessor;
        this.authValidationManager = authValidationManager;
        this.streamStorage = streamStorage;

        this.async = async;
        this.maxContentLength = maxContentLength;

        this.time = time;

        //TODO: Move throttling duration metric to Throttle
        this.requestThrottleDurationMsTimer = metricsCollector.timer("requestThrottleDurationMs");
    }

    @Override
    public void handle(HttpServerRequest request) {
        final long requestTimestampMs = time.milliseconds();

        Parameter<String>.ParameterValue streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (streamName.isError()) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }
        String stream = streamName.get();

        AuthResult authResult = authProvider.authWrite(request, stream);
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        String apiKey = request.getHeader("apiKey");
        // Check content length
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }
        int contentLength = optionalContentLength.get();

        if (contentLength > maxContentLength) {
            request.complete(HttpStatusCodes.REQUEST_ENTITY_TOO_LARGE);
            return;
        }

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

        Set<TinyString> tagsToValidate = authValidationManager.getTags(apiKey, stream);

        ShardingKey shardingKey = ShardingKey.fromKeyPaths(baseStream.getShardingKey());

        Set<TinyString> tags = new HashSet<>(Maps.effectiveHashMapCapacity(shardingKey.size() + tagsToValidate.size()));
        Arrays.stream(shardingKey.getKeys()).map(HPath::getRootTag).forEach(tags::add);//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        tags.addAll(tagsToValidate);

        ContentValidator validator = authValidationManager.validator(apiKey, stream);

        final ThrottleResult throttleResult;
        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(requestThrottleDurationMsTimer, TimeUnit.MILLISECONDS, time)) {
            throttleResult = this.throttle.throttle(request);
        }

        if (throttleResult.reason() != ThrottledBy.NONE) {
            throttledRequestProcessor.process(request, throttleResult.reason());
            return;
        }

        SendRequestContext context =
                new SendRequestContext(requestTimestampMs, async, baseStream, tags, shardingKey, validator);
        sendRequestProcessor.processAsync(request, context, () -> throttle.release(throttleResult));
    }

    private static class QueryParameters {
        //TODO: stream name validation
        public static Parameter<String> STREAM =
                Parameter.stringParameter("stream").
                        required().
                        build();

        private QueryParameters() {
            /* static class*/
        }
    }
}
