package ru.kontur.vostok.hercules.tracing.api;

import ru.kontur.vostok.hercules.tracing.api.cassandra.CassandraTracingReader;
import ru.kontur.vostok.hercules.tracing.api.json.EventToJsonConverter;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.util.UUID;

/**
 * GetTraceHandler
 *
 * @author Gregory Koshelev
 */
public class GetTraceHandler implements HttpHandler {
    private final TracingReader tracingReader;

    public GetTraceHandler(TracingReader tracingReader) {
        this.tracingReader = tracingReader;
    }

    @Override
    public void handle(HttpServerRequest request) {
        ParameterValue<UUID> traceId = QueryUtil.get(QueryParameters.TRACE_ID, request);
        if (traceId.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TRACE_ID.name() + " error: " + traceId.result().error());
            return;
        }

        ParameterValue<UUID> parentSpanId = QueryUtil.get(QueryParameters.PARENT_SPAN_ID, request);
        if (parentSpanId.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.PARENT_SPAN_ID.name() + " error: " + parentSpanId.result().error());
            return;
        }

        ParameterValue<Integer> limit = QueryUtil.get(QueryParameters.LIMIT, request);
        if (limit.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.LIMIT.name() + " error: " + limit.result().error());
            return;
        }

        ParameterValue<String> pagingState = QueryUtil.get(QueryParameters.PAGING_STATE, request);
        if (pagingState.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.PAGING_STATE.name() + " error: " + limit.result().error());
            return;
        }

        if (!parentSpanId.isEmpty()) {
            final Page<Event> traceSpansByTraceIdAndParentSpanId = tracingReader.getTraceSpansByTraceIdAndParentSpanId(
                    traceId.get(),
                    parentSpanId.get(),
                    limit.get(),
                    pagingState.orEmpty(null));

            request.getResponse().setContentType(MimeTypes.APPLICATION_JSON);
            request.getResponse().send(EventToJsonConverter.pagedResultAsString(traceSpansByTraceIdAndParentSpanId));
        } else {
            final Page<Event> traceSpansByTraceId = tracingReader.getTraceSpansByTraceId(
                    traceId.get(),
                    limit.get(),
                    pagingState.orEmpty(null));

            request.getResponse().setContentType(MimeTypes.APPLICATION_JSON);
            request.getResponse().send(EventToJsonConverter.pagedResultAsString(traceSpansByTraceId));
        }
    }
}
