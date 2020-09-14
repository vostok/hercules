package ru.kontur.vostok.hercules.management.api.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

/**
 * @author Gregory Koshelev
 */
public class SetRuleHandler extends RuleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetRuleHandler.class);

    public SetRuleHandler(RuleRepository repository) {
        super(repository);
    }

    @Override
    public void handle(HttpServerRequest request) {
        Parameter<String>.ParameterValue key = QueryUtil.get(QueryParameters.KEY, request);
        Parameter<String>.ParameterValue pattern = QueryUtil.get(QueryParameters.PATTERN, request);
        Parameter<String>.ParameterValue rights = QueryUtil.get(QueryParameters.RIGHTS, request);

        if (key.isError() || pattern.isError() || rights.isError()) {
            request.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        }

        String ruleRead = key.get() + '.' + pattern.get() + '.' + "read";
        String ruleWrite = key.get() + '.' + pattern.get() + '.' + "write";
        String ruleManage = key.get() + '.' + pattern.get() + '.' + "manage";

        String mask = rights.get();

        try {
            if (mask.charAt(0) == 'r') {
                repository.create(ruleRead);
            } else {
                repository.delete(ruleRead);
            }
            if (mask.charAt(1) == 'w') {
                repository.create(ruleWrite);
            } else {
                repository.delete(ruleWrite);
            }
            if (mask.charAt(2) == 'm') {
                repository.create(ruleManage);
            } else {
                repository.delete(ruleManage);
            }
        } catch (CuratorException ex) {
            LOGGER.error("Set rule failed with exception", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        request.complete(HttpStatusCodes.OK);
    }
}
