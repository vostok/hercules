package ru.kontur.hercules.tracing.api;

import com.datastax.driver.core.exceptions.PagingStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.hercules.tracing.api.cassandra.PagedResult;
import ru.kontur.hercules.tracing.api.json.EventToJsonConverter;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parsing.Parsers;

import java.util.Optional;
import java.util.UUID;

/**
 * GetTraceHandler
 *
 * @author Kirill Sulim
 */
public class GetTraceHandler implements HttpHandler {

    private static final int DEFAULT_COUNT = 100;

    private final CassandraTracingReader cassandraTracingReader;
    private final ObjectMapper objectMapper;

    public GetTraceHandler(
        CassandraTracingReader cassandraTracingReader,
        ObjectMapper objectMapper
    ) {
        this.cassandraTracingReader = cassandraTracingReader;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        final Result<UUID, String> traceIdResult = ExchangeUtil.extractQueryParam(exchange, "traceId")
            .map(Result::<String, String>ok)
            .orElse(Result.error("Required parameter missing"))
            .flatMap(Parsers::parseUuid);

        if (!traceIdResult.isOk()) {
            ResponseUtil.badRequest(exchange, String.format("Parameter traceId has illegal value: %s", traceIdResult.getError()));
            return;
        }

        final Result<Integer, String> countResult = ExchangeUtil.extractQueryParam(exchange, "count")
            .map(Parsers::parseInteger)
            .orElse(Result.ok(DEFAULT_COUNT));

        if (!countResult.isOk()) {
            ResponseUtil.badRequest(exchange, String.format("Parameter count has illegal value: %s", countResult.getError()));
        }

        final Optional<String> pagingState = ExchangeUtil.extractQueryParam(exchange, "pagingState");

        try {
            final PagedResult<Event> traceSpansByTraceId = cassandraTracingReader.getTraceSpansByTraceId(
                traceIdResult.get(),
                countResult.get(),
                pagingState.orElse(null)
            );

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(EventToJsonConverter.pagedResultAsString(traceSpansByTraceId));
            exchange.endExchange();
        } catch (PagingStateException e) {
            ResponseUtil.badRequest(exchange, "Incorrect pagingState value");
        }
    }
}
