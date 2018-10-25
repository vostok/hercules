package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SentryClientHolder
 *
 * @author Kirill Sulim
 */
public class SentryClientHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryClientHolder.class);

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";
    private static final String DISABLE_IN_APP_WARN_MESSAGE = DefaultSentryClientFactory.IN_APP_FRAMES_OPTION + "=%20"; // Empty value disables warn message

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
        try {
            Result<List<ProjectInfo>, String> projects = sentryApiClient.getProjects();
            if (!projects.isOk()) {
                LOGGER.error("Cannot update project info due to: {}", projects.getError());
                return;
            }

            Map<String, SentryClient> updatedClients = new HashMap<>();

            for (ProjectInfo projectInfo : projects.get()) {
                Result<List<KeyInfo>, String> publicDsn = sentryApiClient.getPublicDsn(projectInfo);
                if (!publicDsn.isOk()) {
                    LOGGER.error("Cannot get public dns for project '{}' due to: {}", projectInfo.getSlug(), publicDsn.getError());
                    return;
                }

                Optional<String> dsn = publicDsn.get().stream().findAny().map(KeyInfo::getDsn).map(DsnInfo::getPublicDsn);
                if (dsn.isPresent()) {
                    String dsnString = dsn.get();
                    try {
                        new URL(dsnString);
                    }
                    catch (MalformedURLException e) {
                        throw new Exception(String.format("Malformed dsn '%s', there might be an error in sentry configuration", dsnString));
                    }
                    updatedClients.put(String.format("%s/%s", projectInfo.getOrganization().getSlug(), projectInfo.getSlug()), SentryClientFactory.sentryClient(applySettings(dsnString)));
                }
            }

            clients.set(updatedClients);
        }
        catch (Throwable t) {
            LOGGER.error("Error in scheduled thread", t);
            System.exit(1);
        }
    }

    /*
     * Sentry uses dsn to pass properties to client
     */
    private String applySettings(String dsn) {
        return dsn + "?" + String.join("&",
                DISABLE_UNCAUGHT_EXCEPTION_HANDLING,
                DISABLE_IN_APP_WARN_MESSAGE
        );
    }
}
