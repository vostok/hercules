package ru.kontur.vostok.hercules.sentry.sink;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectMappingRecord;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.util.schedule.RenewableTask;
import ru.kontur.vostok.hercules.util.schedule.Scheduler;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private volatile Map<ProjectServicePair, String> registry = new HashMap<>();

    public SentryProjectRegistry(SentryProjectRepository sentryProjectRepository) {
        this.sentryProjectRepository = sentryProjectRepository;

        this.scheduler = new Scheduler(1);
        this.updateTask = scheduler.task(this::update, 60_000, false);
    }

    public Optional<String> getSentryProjectName(@NotNull final String project, @Nullable final String service) {
        return Optional.ofNullable(registry.get(ProjectServicePair.of(project, service)));
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
                    SentryProjectRegistry::getKey,
                    SentryProjectRegistry::getValue
                ));
        });
    }

    private static ProjectServicePair getKey(final SentryProjectMappingRecord record) {
        return ProjectServicePair.of(record.getProject(), record.getService());
    }

    private static String getValue(final SentryProjectMappingRecord record) {
        return record.getSentryOrganization() + "/" + record.getSentryProject();
    }

    private static class ProjectServicePair {
        private final String project;
        private final String service;

        public ProjectServicePair(@NotNull final String project, @Nullable String service) {
            this.project = project;
            this.service = service;
        }

        public String getProject() {
            return project;
        }

        public String getService() {
            return service;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectServicePair that = (ProjectServicePair) o;
            return Objects.equals(project, that.project) &&
                Objects.equals(service, that.service);
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, service);
        }

        public static ProjectServicePair of(@NotNull final String project, @Nullable final String service) {
            return new ProjectServicePair(project, service);
        }
    }
}
