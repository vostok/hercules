package ru.kontur.vostok.hercules.gateway;

import com.codahale.metrics.Meter;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.Action;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
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

    protected final EventSender eventSender;
    protected final UuidGenerator uuidGenerator;

    protected final Meter requestMeter;
    protected final Meter requestSizeMeter;
    protected final Meter sentEventsMeter;

    protected GatewayHandler(MetricsCollector metricsCollector, AuthManager authManager, EventSender eventSender, StreamRepository streamRepository) {
        this.metricsCollector = metricsCollector;

        this.authManager = authManager;
        this.eventSender = eventSender;
        this.streamRepository = streamRepository;
        this.uuidGenerator = UuidGenerator.getInternalInstance();

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

        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }
        if (!auth(exchange, apiKey.get(), stream)) {
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

        String[] shardingKey = baseStream.getShardingKey();
        int partitions = baseStream.getPartitions();
        String topic = baseStream.getName();
        Set<String> tags = new HashSet<>(shardingKey.length);
        tags.addAll(Arrays.asList(shardingKey));

        Marker marker = Marker.forKey(apiKey.get());
        send(exchange, marker, topic, tags, partitions, shardingKey);
    }

    protected abstract void send(HttpServerExchange exchange, Marker marker, String topic, Set<String> tags, int partitions, String[] shardingKey);

    protected EventSender getEventSender() {
        return eventSender;
    }

    private boolean auth(HttpServerExchange exchange, String apiKey, String stream) {
        AuthResult authResult = authManager.auth(apiKey, stream, Action.WRITE);

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
