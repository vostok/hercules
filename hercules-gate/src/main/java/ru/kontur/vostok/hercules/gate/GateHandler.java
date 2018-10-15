package ru.kontur.vostok.hercules.gate;

import com.codahale.metrics.Meter;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.throttling.Throttle;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.uuid.Marker;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

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
    private final Throttle<HttpServerExchange, SendContext> throttle;
    private final StreamStorage streamStorage;
    private final AuthValidationManager authValidationManager;

    private final UuidGenerator uuidGenerator;

    private final boolean async;

    private final Meter requestMeter;
    private final Meter requestSizeMeter;

    public GateHandler(
            MetricsCollector metricsCollector,
            AuthManager authManager,
            Throttle<HttpServerExchange, SendContext> throttle,
            AuthValidationManager authValidationManager,
            StreamStorage streamStorage,
            boolean async
    ) {
        this.metricsCollector = metricsCollector;

        this.authManager = authManager;
        this.throttle = throttle;
        this.authValidationManager = authValidationManager;
        this.streamStorage = streamStorage;
        this.uuidGenerator = UuidGenerator.getInternalInstance();

        this.async = async;

        this.requestMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".request");
        this.requestSizeMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".request_size");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        requestMeter.mark(1);

        Optional<String> optionalStream = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!optionalStream.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        String stream = optionalStream.get();
        //TODO: stream name validation

        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }
        String apiKey = optionalApiKey.get();
        if (!auth(exchange, apiKey, stream)) {
            return;
        }

        // Check content length
        int contentLength = ExchangeUtil.extractContentLength(exchange);
        if (contentLength < 0 || contentLength > (1 << 21)) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        requestSizeMeter.mark(contentLength);

        Optional<Stream> optionalBaseStream = streamStorage.read(stream);
        if (!optionalBaseStream.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }
        Stream baseStream = optionalBaseStream.get();
        if (!(baseStream instanceof BaseStream)) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        Set<String> tagsToValidate = authValidationManager.getTags(apiKey, stream);

        String[] shardingKey = baseStream.getShardingKey();
        int partitions = baseStream.getPartitions();
        String topic = baseStream.getName();

        Set<String> tags = new HashSet<>(shardingKey.length + tagsToValidate.size());
        tags.addAll(Arrays.asList(shardingKey));
        tags.addAll(tagsToValidate);

        Marker marker = Marker.forKey(apiKey);
        ContentValidator validator = authValidationManager.validator(apiKey, stream);

        SendContext context = new SendContext(async, marker, topic, tags, partitions, shardingKey, validator);
        throttle.throttleAsync(exchange, context);
    }

    private boolean auth(HttpServerExchange exchange, String apiKey, String stream) {
        AuthResult authResult = authManager.authWrite(apiKey, stream);

        if (authResult.isSuccess()) {
            return true;
        }

        if (authResult.isUnknown()) {
            ResponseUtil.unauthorized(exchange);
            return false;
        }

        ResponseUtil.forbidden(exchange);
        return false;
    }
}
