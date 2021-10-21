package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.wrapper.AdminAuthHandlerWrapper;
import ru.kontur.vostok.hercules.auth.wrapper.AuthHandlerWrapper;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.AddBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.ListBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.RemoveBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.rule.ListRuleHandler;
import ru.kontur.vostok.hercules.management.api.rule.SetRuleHandler;
import ru.kontur.vostok.hercules.management.api.stream.ChangeStreamDescriptionHandler;
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
import ru.kontur.vostok.hercules.http.handler.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class ManagementApiApplication {
    private static CuratorClient curatorClient;
    private static MetricsCollector metricsCollector;
    private static AuthManager authManager;
    private static AdminAuthManager adminAuthManager;
    private static TaskQueue<StreamTask> streamTaskQueue;
    private static TaskQueue<TimelineTask> timelineTaskQueue;

    public static void main(String[] args) {
        Application.run("Hercules Management API", "management-api", args, (properties, container) -> {
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            curatorClient = container.register(new CuratorClient(curatorProperties));

            metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);

            authManager = container.register(new AuthManager(curatorClient));
            adminAuthManager = new AdminAuthManager(Stream.of(PropertiesUtil.get(Props.ADMIN_KEYS, properties).get()).collect(Collectors.toSet()));

            streamTaskQueue = container.register(new TaskQueue<>(new StreamTaskRepository(curatorClient), 500L));
            timelineTaskQueue = container.register(new TaskQueue<>(new TimelineTaskRepository(curatorClient), 500L));

            container.register(createHttpServer(httpserverProperties));
        });
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        StreamRepository streamRepository = new StreamRepository(curatorClient);
        TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

        BlacklistRepository blacklistRepository = new BlacklistRepository(curatorClient);
        RuleRepository ruleRepository = new RuleRepository(curatorClient);

        AuthProvider authProvider = new AuthProvider(adminAuthManager, authManager, metricsCollector);
        AdminAuthHandlerWrapper adminAuthHandlerWrapper = new AdminAuthHandlerWrapper(authProvider);
        AuthHandlerWrapper authHandlerWrapper = new AuthHandlerWrapper(authProvider);

        HttpHandler createStreamHandler = authHandlerWrapper.wrap(
                new CreateStreamHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler deleteStreamHandler = authHandlerWrapper.wrap(
                new DeleteStreamHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler changeStreamTtlHandler = authHandlerWrapper.wrap(
                new ChangeStreamTtlHandler(authProvider, streamTaskQueue, streamRepository));
        HttpHandler changeStreamDescriptionHandler = authHandlerWrapper.wrap(
                new ChangeStreamDescriptionHandler(authProvider, streamTaskQueue, streamRepository));
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
                post("/streams/changeDescription", changeStreamDescriptionHandler).
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

    private static class Props {
        static final Parameter<String[]> ADMIN_KEYS =
                Parameter.stringArrayParameter("keys").
                        required().
                        build();
    }
}
