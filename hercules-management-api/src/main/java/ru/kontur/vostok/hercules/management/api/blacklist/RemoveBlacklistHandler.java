package ru.kontur.vostok.hercules.management.api.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

/**
 * @author Gregory Koshelev
 */
public class RemoveBlacklistHandler extends BlacklistHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveBlacklistHandler.class);

    public RemoveBlacklistHandler(BlacklistRepository repository) {
        super(repository);
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<String>.ParameterValue key = QueryUtil.get(QueryParameters.KEY, request);
        if (QueryUtil.tryCompleteRequestIfError(request, key)) {
            return;
        }

        try {
            repository.remove(key.get());
        } catch (CuratorException ex) {
            LOGGER.error("Add key to blacklist error", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        request.complete(HttpStatusCodes.OK);
    }
}
