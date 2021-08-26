package ru.kontur.vostok.hercules.tracing.api;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GetTraceHandler
 *
 * @author Gregory Koshelev
 */
public class GetTraceHandler implements HttpHandler {
    private final TracingReader tracingReader;
    private final EventToJsonFormatter eventFormatter;

    public GetTraceHandler(TracingReader tracingReader, EventToJsonFormatter eventFormatter) {
        this.tracingReader = tracingReader;
        this.eventFormatter = eventFormatter;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<UUID>.ParameterValue traceId = QueryUtil.get(QueryParameters.TRACE_ID, request);
        if (QueryUtil.tryCompleteRequestIfError(request, traceId)) {
            return;
        }

        Parameter<UUID>.ParameterValue parentSpanId = QueryUtil.get(QueryParameters.PARENT_SPAN_ID, request);
        if (QueryUtil.tryCompleteRequestIfError(request, parentSpanId)) {
            return;
        }

        Parameter<Integer>.ParameterValue limit = QueryUtil.get(QueryParameters.LIMIT, request);
        if (QueryUtil.tryCompleteRequestIfError(request, limit)) {
            return;
        }

        Parameter<String>.ParameterValue pagingState = QueryUtil.get(QueryParameters.PAGING_STATE, request);
        if (QueryUtil.tryCompleteRequestIfError(request, pagingState)) {
            return;
        }

        final Page<Event> traceSpans;
        if (!parentSpanId.isEmpty()) {
            traceSpans = tracingReader.getTraceSpansByTraceIdAndParentSpanId(
                    traceId.get(),
                    parentSpanId.get(),
                    limit.get(),
                    pagingState.orEmpty(null));
        } else {
            traceSpans = tracingReader.getTraceSpansByTraceId(
                    traceId.get(),
                    limit.get(),
                    pagingState.orEmpty(null));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            DocumentWriter.writeTo(outputStream, createDocument(traceSpans));
        } catch (IOException ex) {
            request.complete(
                    HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
        request.getResponse().setContentType(MimeTypes.APPLICATION_JSON);
        request.getResponse().send(byteBuffer);
    }

    private Document createDocument(Page<Event> page) {
        Document document = new Document();
        if (page.state() != null) {
            document.putIfAbsent("pagingState", page.state());
        }
        List<Map<String, Object>> eventList = new LinkedList<>();
        document.putIfAbsent("result", eventList);
        for (Event event : page.elements()) {
            Document eventDocument = eventFormatter.format(event);
            eventList.add(eventDocument.document());
        }
        return document;
    }
}
