package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SentryClientHolder
 *
 * @author Kirill Sulim
 */
public class SentryClientHolder {

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";

    private final AtomicReference<Map<String, SentryClient>> clients = new AtomicReference<>(Collections.emptyMap());
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private final SentryApiClient sentryApiClient;

    public SentryClientHolder(SentryApiClient sentryApiClient) {
        this.sentryApiClient = sentryApiClient;
        this.scheduledExecutor.scheduleAtFixedRate(this::update, 0,10000, TimeUnit.MILLISECONDS);
    }

    // TODO: Add default client
    public Optional<SentryClient> getClient(String name) {
        return Optional.ofNullable(clients.get().get(name));
    }

    private void update() {
        Result<List<ProjectInfo>, String> projects = sentryApiClient.getProjects();
        if (!projects.isOk()) {
            // TODO: logging
            System.out.println(projects.getError());
        }

        Map<String, SentryClient> updatedClients = new HashMap<>();

        for (ProjectInfo projectInfo : projects.get()) {
            Result<List<KeyInfo>, String> publicDsn = sentryApiClient.getPublicDsn(projectInfo);
            if (!publicDsn.isOk()) {
                // TODO: logging
                System.out.println(publicDsn.getError());
                return;
            }

            Optional<String> dsn = publicDsn.get().stream().findAny().map(KeyInfo::getDsn).map(DsnInfo::getPublicDsn);
            if (dsn.isPresent()) {
                updatedClients.put(String.format("%s/%s", projectInfo.getOrganization().getSlug(), projectInfo.getSlug()), SentryClientFactory.sentryClient(patchDsn(dsn.get())));
            }
        }

        clients.set(updatedClients);
    }

    private String patchDsn(String dsn) {
        return dsn + "?" + DISABLE_UNCAUGHT_EXCEPTION_HANDLING;
    }
}
