package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * DeleteProjectHandler
 *
 * @author Kirill Sulim
 */
public class DeleteProjectHandler extends SentryRegistryHandler {

    public DeleteProjectHandler(SentryProjectRepository sentryProjectRepository) {
        super(sentryProjectRepository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {

        final Optional<String> projectName = ExchangeUtil.extractQueryParam(exchange, "project");
        if (!projectName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        final String service = ExchangeUtil.extractQueryParam(exchange, "service").orElse(null);

        sentryProjectRepository.delete(projectName.get(), service);
        ResponseUtil.ok(exchange);
    }
}
