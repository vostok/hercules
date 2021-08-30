package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;
import ru.kontur.vostok.hercules.http.path.Path;
import ru.kontur.vostok.hercules.http.path.PathTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public final class RouteHandler implements HttpHandler {
    private final Map<HttpMethod, HandlerMatchers> handlers;

    RouteHandler(Map<HttpMethod, HandlerMatchers> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(HttpServerRequest request) {
        String path = request.getPath();
        HttpMethod method;
        try {
            method = request.getMethod();
        } catch (NotSupportedHttpMethodException e) {
            request.complete(HttpStatusCodes.METHOD_NOT_ALLOWED);
            return;
        }

        HandlerMatchers matchers = handlers.get(method);
        if (matchers == null) {
            request.complete(HttpStatusCodes.METHOD_NOT_ALLOWED);
            return;
        }

        for (Map.Entry<PathTemplate.ExactPathMatcher, HttpHandler> matcher : matchers.exactMatchers.entrySet()) {
            if (matcher.getKey().match(path)) {
                matcher.getValue().handle(request);
                return;
            }
        }

        Path thePath = Path.of(path);
        for (Map.Entry<PathTemplate.PathTemplateMatcher, HttpHandler> matcher : matchers.templateMatchers.entrySet()) {
            Map<String, String> pathParameters = matcher.getKey().match(thePath);
            if (!pathParameters.isEmpty()) {
                request.setPathParameters(pathParameters);
                matcher.getValue().handle(request);
                return;
            }
        }

        request.complete(HttpStatusCodes.NOT_FOUND, ContentTypes.TEXT_PLAIN_UTF_8, "Page not found");
    }

    static class HandlerMatchers {
        private final Map<PathTemplate.ExactPathMatcher, HttpHandler> exactMatchers = new LinkedHashMap<>();
        private final Map<PathTemplate.PathTemplateMatcher, HttpHandler> templateMatchers = new LinkedHashMap<>();

        HandlerMatchers() {

        }

        public void addMatching(PathTemplate pathTemplate, HttpHandler httpHandler) {
            if (pathTemplate.isExactPath()) {
                exactMatchers.put(pathTemplate.toExactMatcher(), httpHandler);
            } else {
                templateMatchers.put(pathTemplate.toMatcher(), httpHandler);
            }
        }
    }

}
