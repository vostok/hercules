package ru.kontur.vostok.hercules.meta.auth.rule;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class RuleRepository {
    private final CuratorClient curatorClient;

    public RuleRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public List<String> list() throws CuratorException {
        List<String> rules = curatorClient.children(zPrefix);
        return rules;
    }

    public void create(String rule) throws CuratorException {
        curatorClient.createIfAbsent(zPrefix + "/" + rule);
    }

    public void delete(String rule) throws CuratorException {
        curatorClient.delete(zPrefix + "/" + rule);
    }

    private static String zPrefix = "/hercules/auth/rules";
}
