package ru.kontur.vostok.hercules.management.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.management.api.auth.AdminAuthHandlerWrapper;
import ru.kontur.vostok.hercules.management.api.auth.AuthHandlerWrapper;
import ru.kontur.vostok.hercules.management.api.auth.AuthProvider;
import ru.kontur.vostok.hercules.management.api.blacklist.AddBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.ListBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.RemoveBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.rule.ListRuleHandler;
import ru.kontur.vostok.hercules.management.api.rule.SetRuleHandler;
import ru.kontur.vostok.hercules.management.api.stream.ChangeStreamTtlHandler;
import ru.kontur.vostok.hercules.management.api.stream.CreateStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.DeleteStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.IncreasePartitionsStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.InfoStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.ListStreamHandler;
import ru.kontur.vostok.hercules.management.api.timeline.ChangeTimelineTtlHandler;
import ru.kontur.vostok.hercules.management.api.timeline.CreateTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.DeleteTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.InfoTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.ListTimelineHandler;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class ManagementApiApplication {

    private static class Props {
        static final PropertyDescription<Set<String>> ADMIN_KEYS = PropertyDescriptions
                .setOfStringsProperty("keys")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementApiApplication.class);

    private static CuratorClient curatorClient;
    private static MetricsCollector metricsCollector;
    private static AuthManager authManager;
    private static AdminAuthManager adminAuthManager;
    private static TaskQueue<StreamTask> streamTaskQueue;
    private static TaskQueue<TimelineTask> timelineTaskQueue;
    private static HttpServer server;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Management API", "management-api", args);

            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
            ApplicationContextHolder.init("Hercules management API", "management-api", contextProperties);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            authManager = new AuthManager(curatorClient);
            authManager.start();

            adminAuthManager = new AdminAuthManager(Props.ADMIN_KEYS.extract(properties));

            streamTaskQueue = new TaskQueue<>(new StreamTaskRepository(curatorClient), 500L);
            timelineTaskQueue = new TaskQueue<>(new TimelineTaskRepository(curatorClient), 500L);

            server = createHttpServer(httpserverProperties);
            server.start();
        } catch (Throwable e) {
            LOGGER.error("Error on starting management api", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(ManagementApiApplication::shutdown));

        LOGGER.info("Management API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Management API shutdown");
        try {
            if (server != null) {
                server.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on server stopping", e);
            //TODO: Process error
        }

        try {
            if (timelineTaskQueue != null) {
                timelineTaskQueue.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on timeline's task queue stopping", e);
        }

        try {
            if (streamTaskQueue != null) {
                streamTaskQueue.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on stream's task queue stopping", e);
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on auth manager stopping", e);
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on metrics collector stopping", e);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on curator client stopping", e);
            //TODO: Process error
        }

        LOGGER.info("Finished Management API shutdown for {} millis", System.currentTimeMillis() - start);
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        StreamRepository streamRepository = new StreamRepository(curatorClient);
        TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

        BlacklistRepository blacklistRepository = new BlacklistRepository(curatorClient);
        RuleRepository ruleRepository = new RuleRepository(curatorClient);

        AuthProvider authProvider = new AuthProvider(adminAuthManager, authManager);
        AdminAuthHandlerWrapper adminAuthHandlerWrapper = new AdminAuthHandlerWrapper(authProvider);
        AuthHandlerWrapper authHandlerWrapper = new AuthHandlerWrapper(authProvider);

        HttpHandler createStreamHandler = authHandlerWrapper.wrap(
                new CreateStreamHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler deleteStreamHandler = authHandlerWrapper.wrap(
                new DeleteStreamHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler changeStreamTtlHandler = authHandlerWrapper.wrap(
                new ChangeStreamTtlHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler increasePartitionsStreamHandler = authHandlerWrapper.wrap(
                new IncreasePartitionsStreamHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler listStreamHandler = authHandlerWrapper.wrap(
                new ListStreamHandler(streamRepository));
        HttpHandler infoStreamHandler = authHandlerWrapper.wrap(
                new InfoStreamHandler(streamRepository, authProvider));

        HttpHandler createTimelineHandler = authHandlerWrapper.wrap(
                new CreateTimelineHandler(authProvider, timelineTaskQueue, timelineRepository));
        HttpHandler deleteTimelineHandler = authHandlerWrapper.wrap(
                new DeleteTimelineHandler(authProvider, timelineTaskQueue, timelineRepository));
        HttpHandler changeTimelineTtlHandler = authHandlerWrapper.wrap(
                new ChangeTimelineTtlHandler(authProvider, timelineTaskQueue, timelineRepository));
        HttpHandler listTimelineHandler = authHandlerWrapper.wrap(
                new ListTimelineHandler(timelineRepository));
        HttpHandler infoTimelineHandler = authHandlerWrapper.wrap(
                new InfoTimelineHandler(timelineRepository, authProvider));

        HttpHandler setRuleHandler = adminAuthHandlerWrapper.wrap(new SetRuleHandler(ruleRepository));
        HttpHandler listRuleHandler = adminAuthHandlerWrapper.wrap(new ListRuleHandler(ruleRepository));

        HttpHandler addBlacklistHandler = adminAuthHandlerWrapper.wrap(new AddBlacklistHandler(blacklistRepository));
        HttpHandler removeBlacklistHandler = adminAuthHandlerWrapper.wrap(new RemoveBlacklistHandler(blacklistRepository));
        HttpHandler listBlacklistHandler = adminAuthHandlerWrapper.wrap(new ListBlacklistHandler(blacklistRepository));

        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                post("/streams/create", createStreamHandler).
                post("/streams/delete", deleteStreamHandler).
                post("/streams/changeTtl", changeStreamTtlHandler).
                post("/streams/increasePartitions", increasePartitionsStreamHandler).
                get("/streams/list", listStreamHandler).
                get("/streams/info", infoStreamHandler).
                post("/timelines/create", createTimelineHandler).
                post("/timelines/delete", deleteTimelineHandler).
                post("/timelines/changeTtl", changeTimelineTtlHandler).
                get("/timelines/list", listTimelineHandler).
                get("/timelines/info", infoTimelineHandler).
                post("/rules/set", setRuleHandler).
                get("/rules/list", listRuleHandler).
                post("/blacklist/add", addBlacklistHandler).
                post("/blacklist/remove", removeBlacklistHandler).
                get("/blacklist/list", listBlacklistHandler).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
