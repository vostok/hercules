package ru.kontur.vostok.hercules.auth;

import org.apache.zookeeper.Watcher;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.LatchWatcher;
import ru.kontur.vostok.hercules.meta.auth.blacklist.Blacklist;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.concurrent.RenewableTask;
import ru.kontur.vostok.hercules.util.concurrent.RenewableTaskScheduler;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public final class AuthManager implements Lifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthManager.class);

    private static final String PATH = "/hercules/auth/rules";

    private final CuratorClient curatorClient;

    private final RenewableTaskScheduler scheduler;

    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> readRules = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> writeRules = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, List<PatternMatcher>>> manageRules = new AtomicReference<>(new ConcurrentHashMap<>());

    private final Blacklist blacklist;

    private final RenewableTask updateTask;
    private final LatchWatcher latchWatcher;

    public AuthManager(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.scheduler = new RenewableTaskScheduler("auth-manager", 1);

        this.blacklist = new Blacklist(curatorClient, scheduler);

        this.updateTask = scheduler.task(this::update, 60_000, false);
        this.latchWatcher = new LatchWatcher(event -> {
            if (event.getType() == Watcher.Event.EventType.None) {
                // We are only interested in the data changes
                //TODO: Process ZK reconnection separately by using CuratorFramework.getConnectionStateListenable()
                return;
            }
            updateTask.renew();
        });
    }

    @Override
    public void start() {
        blacklist.start();

        updateTask.renew();
    }

    /**
     * @deprecated use {@link #stop(long, TimeUnit)} instead
     */
    public void stop() {
        stop(5_000, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        Timer timer = TimeSource.SYSTEM.timer(unit.toMillis(timeout));//FIXME: better use TimeSource is passed via constructor
        boolean result = blacklist.stop(timer.remainingTimeMs(), TimeUnit.MILLISECONDS);
        updateTask.disable();
        return result && scheduler.stop(timer.remainingTimeMs(), unit);
    }

    /**
     * Check if the provided api key is known.
     *
     * @param apiKey the api key
     * @return {@code true} if the api key exists or {@code false} otherwise
     */
    public boolean hasApiKey(@NotNull String apiKey) {
        return readRules.get().containsKey(apiKey)
                || writeRules.get().containsKey(apiKey)
                || manageRules.get().containsKey(apiKey);
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

    private void update() {
        List<String> rules;
        try {
            rules = latchWatcher.latch() ? curatorClient.children(PATH, latchWatcher) : curatorClient.children(PATH);
        } catch (Exception e) {
            LOGGER.error("Error on getting rules", e);
            return;
        }

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
