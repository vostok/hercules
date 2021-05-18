package ru.kontur.vostok.hercules.sd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationConfig;
import ru.kontur.vostok.hercules.application.ApplicationContext;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUnitUtil;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link BeaconService} is used to automate service registration for Service Discovery over ZooKeeper.
 * <p>
 * {@link BeaconService} uses service instance information from {@link ApplicationContext} and registers it in ZooKeeper.
 * Existence of such a registration (or beacon) means that service instance is an active.
 * Thus, {@link BeaconService} should periodically check if beacon exists and re-register if it did not.
 *
 * @author Gregory Koshelev
 */
public class BeaconService implements Lifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeaconService.class);

    private final Properties properties;
    private final CuratorClient curatorClient;
    private final ScheduledExecutorService executor;

    private volatile long sessionId;

    private String beaconPath;
    private byte[] beaconData;

    public BeaconService(Properties properties, CuratorClient curatorClient) {
        this.properties = properties;
        this.curatorClient = curatorClient;

        this.executor = Executors.newSingleThreadScheduledExecutor(ThreadFactories.newDaemonNamedThreadFactory("beacon"));
    }

    /**
     * Start beacon's registration
     */
    public void start() {
        long periodMs = PropertiesUtil.get(Props.PERIOD_MS, properties).get();

        sessionId = getCurrentSessionId();

        String address = PropertiesUtil.get(Props.ADDRESS, properties).get();
        if ("".equals(address)) {
            ApplicationConfig applicationConfig = Application.application().getConfig();
            address = "http://" + applicationConfig.getHost() + ":" + applicationConfig.getPort();
        }

        ApplicationContext context = Application.context();

        beaconPath = zPrefix + '/' + context.getApplicationId() + '/' + context.getInstanceId();
        BeaconInfo beaconInfo =
                new BeaconInfo(
                        context.getApplicationName(),
                        context.getApplicationId(),
                        context.getVersion(),
                        context.getCommitId(),
                        context.getEnvironment(),
                        context.getZone(),
                        context.getInstanceId(),
                        address);
        try {
            beaconData = new ObjectMapper().writeValueAsString(beaconInfo).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException ex) {
            /* Should never happen */
            LOGGER.error("Cannot serialize ApplicationContext", ex);
            beaconData = new byte[0];
        }

        try {
            register();
        } catch (CuratorException | BeaconConflictException ex) {
            LOGGER.error("Cannot register beacon due to exception", ex);
            throw new IllegalStateException(ex);
        }

        executor.scheduleAtFixedRate(this::tryRegisterIfNeeded, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop beacon registration process
     *
     * @param timeout the timeout to stop
     * @param unit    the unit of time
     * @return {@code true} if successfully stopped, {@code false} otherwise
     */
    public boolean stop(long timeout, TimeUnit unit) {
        boolean disabled = false;
        boolean unregistered = false;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                LOGGER.warn("Did not stop for " + timeout + " " + TimeUnitUtil.toString(unit));
            } else {
                disabled = true;
            }

        } catch (InterruptedException e) {
            LOGGER.warn("Stopping was interrupted", e);
            Thread.currentThread().interrupt();
            disabled = false;
        }

        try {
            curatorClient.delete(beaconPath);
            unregistered = true;
        } catch (CuratorException ex) {
            LOGGER.error("Cannot disable Beacon due to exception", ex);
        }

        return disabled && unregistered;
    }

    /**
     * Register beacon for service
     *
     * @throws CuratorUnknownException  in case of unknown exceptions
     * @throws CuratorInternalException in case of internal Curator exceptions
     * @throws BeaconConflictException  in case of conflict when creates beacon
     */
    private void register() throws CuratorUnknownException, CuratorInternalException, BeaconConflictException {
        try {
            CreationResult result = curatorClient.createWithMode(beaconPath, beaconData, CreateMode.EPHEMERAL);
            if (result.isSuccess()) {
                return;
            }
            LOGGER.error("Beacon already exists");
            throw new BeaconConflictException("Beacon already exists");
        } catch (CuratorException ex) {
            LOGGER.error("Registration fails with exception", ex);
            throw ex;
        }
    }

    /**
     * Try to register beacon if no beacon has been registered before.
     * <p>
     * Periodic execution of this method is scheduled with {@link ScheduledExecutorService}.
     * Thus, it must not throw any exception not to prevent periodic execution.
     */
    private void tryRegisterIfNeeded() {
        try {
            final long ownerSessionId = getBeaconOwnerSessionId();

            if (ownerSessionId == 0) {
                register();
                return;
            }

            if (ownerSessionId == sessionId || ownerSessionId == (sessionId = getCurrentSessionId())) {
                return;
            }

            LOGGER.error("Beacon is registered by another client " + ownerSessionId);
        } catch (Exception ex) {
            LOGGER.error("Registration fails with exception", ex);
        }
    }

    private long getCurrentSessionId() {
        try {
            return curatorClient.getSessionId();
        } catch (CuratorException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private long getBeaconOwnerSessionId() throws CuratorUnknownException {
        CuratorFramework curatorFramework = curatorClient.getCuratorFramework();
        try {
            Stat stat = curatorFramework.checkExists().forPath(beaconPath);
            return stat != null ? stat.getEphemeralOwner() : 0;
        } catch (Exception ex) {
            throw new CuratorUnknownException(ex);
        }
    }

    private static String zPrefix = "/hercules/sd/services";

    private static class Props {
        /**
         * Address of service to access it via http
         */
        static final Parameter<String> ADDRESS =
                Parameter.stringParameter("address").
                        withDefault("").
                        build();
        /**
         * Period of beacon registration check in milliseconds
         */
        static final Parameter<Long> PERIOD_MS =
                Parameter.longParameter("periodMs").
                        withDefault(10_000L).
                        withValidator(LongValidators.positive()).
                        build();
    }
}
