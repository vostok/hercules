package ru.kontur.vostok.hercules.gateway;

import com.codahale.metrics.Meter;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.gateway.validation.EventValidator;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
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
public abstract class GatewayHandler implements HttpHandler {
    protected final MetricsCollector metricsCollector;

    private final AuthManager authManager;
    private final StreamRepository streamRepository;
    private final AuthValidationManager authValidationManager;

    protected final EventSender eventSender;
    protected final UuidGenerator uuidGenerator;
    protected final EventValidator eventValidator;

    protected final Meter requestMeter;
    protected final Meter requestSizeMeter;
    protected final Meter sentEventsMeter;

    protected GatewayHandler(MetricsCollector metricsCollector, AuthManager authManager, AuthValidationManager authValidationManager, EventSender eventSender, StreamRepository streamRepository) {
        this.metricsCollector = metricsCollector;

        this.authManager = authManager;
        this.authValidationManager = authValidationManager;
        this.eventSender = eventSender;
        this.streamRepository = streamRepository;
        this.uuidGenerator = UuidGenerator.getInternalInstance();
        this.eventValidator = new EventValidator();

        this.requestMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".request");
        this.requestSizeMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".request_size");
        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sent_events");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
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

        Optional<Stream> optionalBaseStream = streamRepository.read(stream);
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
        EventValidator validator = authValidationManager.validator(apiKey, stream);
        send(exchange, marker, topic, tags, partitions, shardingKey, validator);
    }

    protected abstract void send(HttpServerExchange exchange, Marker marker, String topic, Set<String> tags, int partitions, String[] shardingKey, EventValidator validator);

    protected EventSender getEventSender() {
        return eventSender;
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
