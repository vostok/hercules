package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectMappingRecord;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.util.schedule.RenewableTask;
import ru.kontur.vostok.hercules.util.schedule.Scheduler;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SentryProjectRegistry stores dictionary project tag value -> "${sentry-organization}/${sentry-project-name}"
 *
 * @author Kirill Sulim
 */
public class SentryProjectRegistry {

    private final Scheduler scheduler;
    private final RenewableTask updateTask;
    private final SentryProjectRepository sentryProjectRepository;

    private volatile Map<String, String> registry = new HashMap<>();

    public SentryProjectRegistry(SentryProjectRepository sentryProjectRepository) {
        this.sentryProjectRepository = sentryProjectRepository;

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
        ThrowableUtil.toUnchecked(() -> {
            registry = sentryProjectRepository.list().stream()
                    .collect(Collectors.toMap(
                            SentryProjectMappingRecord::getProject,
                            SentryProjectMappingRecord::getSentryProject
                    ));
        });
    }
}
