package ru.kontur.vostok.hercules.management.api.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

/**
 * @author Gregory Koshelev
 */
public class AddBlacklistHandler extends BlacklistHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddBlacklistHandler.class);

    public AddBlacklistHandler(BlacklistRepository repository) {
        super(repository);
    }

    @Override
    public void handle(HttpServerRequest request) {
        ParameterValue<String> key = QueryUtil.get(QueryParameters.KEY, request);
        if (key.isError()) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }

        try {
            repository.add(key.get());
        } catch (CuratorException ex) {
            LOGGER.error("Add key to blacklist error", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        request.complete(HttpStatusCodes.OK);
    }
}
