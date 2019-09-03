package ru.kontur.vostok.hercules.management.api.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.undertow.util.HttpResponseContentWriter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListBlacklistHandler extends BlacklistHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListBlacklistHandler.class);

    public ListBlacklistHandler(BlacklistRepository repository) {
        super(repository);
    }

    @Override
    public void handle(HttpServerRequest request) {
        List<String> list;
        try {
            list = repository.list().stream()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (CuratorException ex) {
            LOGGER.error("List blacklist error", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpResponseContentWriter.writeJson(list, request);
    }
}
