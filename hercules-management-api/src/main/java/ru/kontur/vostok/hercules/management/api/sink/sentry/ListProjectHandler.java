package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectMappingRecord;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;

import java.util.List;

/**
 * ListProjectHandler
 *
 * @author Kirill Sulim
 */
public class ListProjectHandler extends SentryRegistryHandler {

    public ListProjectHandler(SentryProjectRepository sentryProjectRepository) {
        super(sentryProjectRepository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        List<SentryProjectMappingRecord> mappingList = sentryProjectRepository.list();
        exchange.getResponseSender().send(mapper.writeValueAsString(mappingList));
    }
}
