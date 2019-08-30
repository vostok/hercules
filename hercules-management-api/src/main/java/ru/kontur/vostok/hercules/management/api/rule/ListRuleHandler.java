package ru.kontur.vostok.hercules.management.api.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.undertow.util.HttpResponseContentWriter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListRuleHandler extends RuleHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListRuleHandler.class);

    public ListRuleHandler(RuleRepository repository) {
        super(repository);
    }

    @Override
    public void process(HttpServerRequest request) {
        List<String> list;
        try {
            list = repository.list().stream()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when get children", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpResponseContentWriter.writeJson(list, request);
    }
}
