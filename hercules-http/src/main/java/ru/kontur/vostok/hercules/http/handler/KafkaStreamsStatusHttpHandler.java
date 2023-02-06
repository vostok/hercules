package ru.kontur.vostok.hercules.http.handler;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * Handle Http-requests by depends on state of Kafka streams.
 * @author Tatyana Tokmyanina
 */
public class KafkaStreamsStatusHttpHandler implements HttpHandler {

    private final AtomicReference<State> state;

    public KafkaStreamsStatusHttpHandler(AtomicReference<KafkaStreams.State> state) {
        this.state = state;
    }

    @Override
    public void handle(HttpServerRequest request) {
        int code;
        switch (state.get()) {
            case CREATED:
                code = HttpStatusCodes.SERVICE_UNAVAILABLE;
                break;
            case REBALANCING:
            case RUNNING:
                code = HttpStatusCodes.OK;
                break;
            case PENDING_SHUTDOWN:
                code = HttpStatusCodes.GONE;
                break;
            default:
            case ERROR:
            case NOT_RUNNING:
                code = HttpStatusCodes.INTERNAL_SERVER_ERROR;
                break;
        }
        request.complete(code);
    }
}
