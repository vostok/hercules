package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectMappingRecord;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.Optional;

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

        Optional<Integer> optionalContentLength = ExchangeUtil.extractContentLength(exchange);
        if (!optionalContentLength.isPresent()) {
            ResponseUtil.lengthRequired(exchange);
            return;
        }
        int contentLength = optionalContentLength.get();
        if (contentLength < 0) {
            ResponseUtil.badRequest(exchange);
            return;
        }

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
