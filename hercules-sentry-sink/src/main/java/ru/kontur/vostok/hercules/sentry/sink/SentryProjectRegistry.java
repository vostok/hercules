package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.util.schedule.RenewableTask;
import ru.kontur.vostok.hercules.util.schedule.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * SentryProjectRegistry stores dictionary project tag value -> "${sentry-organization}/${sentry-project-name}"
 *
 * @author Kirill Sulim
 */
public class SentryProjectRegistry {

    private static final String SENTRY_REGISTRY_RECORDS = "/hercules/sink/sentry/registry";

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryProjectRegistry.class);

    private final Scheduler scheduler;
    private final RenewableTask updateTask;
    private CuratorClient curatorClient;

    private volatile Map<String, String> registry = new HashMap<>();

    public SentryProjectRegistry(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.scheduler = new Scheduler(1);
        this.updateTask = scheduler.task(this::update, 60_000, false);
    }

    public Optional<String> getSentryProjectName(String projectTagValue) {
        return Optional.ofNullable(registry.get(projectTagValue));
    }

    public void start() {
        updateTask.renew();
    }

    public void stop() {
        updateTask.disable();
        scheduler.shutdown(5_000, TimeUnit.MILLISECONDS);
    }

    public void update() {
        List<String> records;
        try {
            records = curatorClient.children(SENTRY_REGISTRY_RECORDS, e -> {
                updateTask.renew();
            });//TODO: monitor watcher's event types
        } catch (Exception e) {
            LOGGER.error("Error on updating registry records", e);
            return;
        }

        Map<String, String> result = new HashMap<>();
        for (String record : records) {
            String[] split = record.split(":");
            if (split.length != 3) {
                LOGGER.warn("Invalid sentry registry record: '{}'", record);
            }

            result.put(split[0], split[1] + "/" + split[2]);
        }

        registry = result;
    }
}
