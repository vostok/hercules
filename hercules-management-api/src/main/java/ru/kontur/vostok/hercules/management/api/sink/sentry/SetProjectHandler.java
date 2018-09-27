package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectMappingRecord;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

/**
 * SetProjectHandler
 *
 * @author Kirill Sulim
 */
public class SetProjectHandler extends SentryRegistryHandler {

    public SetProjectHandler(SentryProjectRepository sentryProjectRepository) {
        super(sentryProjectRepository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        exchange.getRequestReceiver().receiveFullBytes(
                (exc, message) -> ThrowableUtil.toUnchecked(() -> {
                    SentryProjectMappingRecord mappingRecord = mapper.readValue(message, SentryProjectMappingRecord.class);
                    sentryProjectRepository.add(mappingRecord);
                    ResponseUtil.ok(exchange);
                }),
                (exc, e) -> ResponseUtil.badRequest(exc)
        );
    }
}
