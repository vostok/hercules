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

        Optional<String> projectName = ExchangeUtil.extractQueryParam(exchange, "projectName");
        if (!projectName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        sentryProjectRepository.delete(projectName.get());
        ResponseUtil.ok(exchange);
    }
}
