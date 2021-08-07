package ru.kontur.vostok.hercules.management.api.timeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.HttpResponseContentWriter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoTimelineHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfoTimelineHandler.class);

    private final AuthProvider authProvider;
    private final TimelineRepository repository;

    public InfoTimelineHandler(TimelineRepository repository, AuthProvider authProvider) {
        this.repository = repository;
        this.authProvider = authProvider;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<String>.ParameterValue timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (QueryUtil.tryCompleteRequestIfError(request, timelineName)) {
            return;
        }

        AuthResult authResult = authProvider.authManage(request, timelineName.get());
        if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
            return;
        }

        Timeline timeline;
        try {
            Optional<Timeline> optionalTimeline = repository.read(timelineName.get());
            if (!optionalTimeline.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            timeline = optionalTimeline.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpResponseContentWriter.writeJson(timeline, request);
    }
}
