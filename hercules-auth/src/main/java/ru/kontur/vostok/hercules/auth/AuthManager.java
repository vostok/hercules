package ru.kontur.vostok.hercules.auth;

import ru.kontur.vostok.hercules.meta.blacklist.Blacklist;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.util.application.shutdown.Stoppable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public final class AuthManager implements Stoppable {
    private final CuratorClient curatorClient;

    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> readRules = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> writeRules = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> manageRules = new AtomicReference<>(new ConcurrentHashMap<>());

    private final Blacklist blacklist;

    public AuthManager(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.blacklist = new Blacklist(curatorClient);
    }

    public void start() throws Exception {
        blacklist.start();

        update();
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        blacklist.stop();
    }

    public AuthResult authRead(String apiKey, String name) {
        return auth(apiKey, name, readRules.get());
    }

    public AuthResult authWrite(String apiKey, String name) {
        return auth(apiKey, name, writeRules.get());
    }

    public AuthResult authManage(String apiKey, String name) {
        return auth(apiKey, name, manageRules.get());
    }

    private AuthResult auth(String apiKey, String name, ConcurrentHashMap<String, List<PatternMatcher>> rules) {
        if (blacklist.contains(apiKey)) {
            return AuthResult.denied();
        }

        List<PatternMatcher> matchers = rules.get(apiKey);
        if (matchers == null) {
            return AuthResult.unknown();
        }

        return PatternMatcher.matchesAnyOf(name, matchers) ? AuthResult.ok() : AuthResult.denied();
    }

    private void update() throws Exception {
        List<String> rules = curatorClient.children("/hercules/auth/rules", e -> update());//TODO: monitor watcher's event types

        ConcurrentHashMap<String, List<PatternMatcher>> newReadRules = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<PatternMatcher>> newWriteRules = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<PatternMatcher>> newManageRules = new ConcurrentHashMap<>();

        for (String rule : rules) {
            String[] split = rule.split("\\.");
            if (split.length != 3) {
                continue;
            }
            String apiKey = split[0];
            String pattern = split[1];
            String right = split[2];

            switch (right) {
                case "read":
                    insertRule(newReadRules, apiKey, pattern);
                    break;
                case "write":
                    insertRule(newWriteRules, apiKey, pattern);
                    break;
                case "manage":
                    insertRule(newManageRules, apiKey, pattern);
                    break;
            }
        }

        readRules.set(newReadRules);
        writeRules.set(newWriteRules);
        manageRules.set(newManageRules);
    }

    private void insertRule(ConcurrentHashMap<String, List<PatternMatcher>> rules, String apiKey, String pattern) {
        List<PatternMatcher> matchers = rules.computeIfAbsent(apiKey, (k) -> new ArrayList<>());
        matchers.add(new PatternMatcher(pattern));
    }
}
