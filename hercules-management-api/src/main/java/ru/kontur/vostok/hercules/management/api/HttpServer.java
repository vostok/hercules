package ru.kontur.vostok.hercules.management.api;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.management.api.blacklist.AddBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.ListBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.RemoveBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.rule.ListRuleHandler;
import ru.kontur.vostok.hercules.management.api.rule.SetRuleHandler;
import ru.kontur.vostok.hercules.management.api.stream.IncreasePartitionsStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.InfoStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.CreateStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.DeleteStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.ListStreamHandler;
import ru.kontur.vostok.hercules.management.api.timeline.CreateTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.DeleteTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.InfoTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.ListTimelineHandler;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.authorization.AdminAuthManagerWrapper;
import ru.kontur.vostok.hercules.undertow.util.handlers.HerculesRoutingHandler;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class HttpServer {

    private static class Props {
        static final PropertyDescription<String> HOST = PropertyDescriptions
                .stringProperty("host")
                .withDefaultValue("0.0.0.0")
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withDefaultValue(6309)
                .build();
    }

    private final Undertow undertow;

    public HttpServer(
            Properties properties,
            AdminAuthManager adminAuthManager,
            AuthManager authManager,
            StreamRepository streamRepository,
            TimelineRepository timelineRepository,
            TaskQueue<StreamTask> streamTaskQueue,
            TaskQueue<TimelineTask> timelineTaskQueue,
            BlacklistRepository blacklistRepository,
            RuleRepository ruleRepository,
            MetricsCollector metricsCollector
    ) {
        final String host = Props.HOST.extract(properties);
        final int port = Props.PORT.extract(properties);

        AdminAuthManagerWrapper adminAuthManagerWrapper = new AdminAuthManagerWrapper(adminAuthManager);

        CreateStreamHandler createStreamHandler = new CreateStreamHandler(authManager, streamTaskQueue, streamRepository);
        DeleteStreamHandler deleteStreamHandler = new DeleteStreamHandler(authManager, streamTaskQueue, streamRepository);
        IncreasePartitionsStreamHandler increasePartitionsStreamHandler =
                new IncreasePartitionsStreamHandler(authManager, streamTaskQueue, streamRepository);
        ListStreamHandler listStreamHandler = new ListStreamHandler(streamRepository);
        InfoStreamHandler infoStreamHandler = new InfoStreamHandler(streamRepository, authManager);

        CreateTimelineHandler createTimelineHandler = new CreateTimelineHandler(authManager, timelineTaskQueue, timelineRepository);
        DeleteTimelineHandler deleteTimelineHandler = new DeleteTimelineHandler(authManager, timelineTaskQueue, timelineRepository);
        ListTimelineHandler listTimelineHandler = new ListTimelineHandler(timelineRepository);
        InfoTimelineHandler infoTimelineHandler = new InfoTimelineHandler(timelineRepository, authManager);

        HttpHandler setRuleHandler = adminAuthManagerWrapper.wrap(new SetRuleHandler(ruleRepository));
        HttpHandler listRuleHandler = adminAuthManagerWrapper.wrap(new ListRuleHandler(ruleRepository));

        HttpHandler addBlacklistHandler = adminAuthManagerWrapper.wrap(new AddBlacklistHandler(blacklistRepository));
        HttpHandler removeBlacklistHandler = adminAuthManagerWrapper.wrap(new RemoveBlacklistHandler(blacklistRepository));
        HttpHandler listBlacklistHandler = adminAuthManagerWrapper.wrap(new ListBlacklistHandler(blacklistRepository));

        HttpHandler handler = new HerculesRoutingHandler(metricsCollector)
                .post("/streams/create", createStreamHandler)
                .post("/streams/delete", deleteStreamHandler)
                .post("/streams/increasePartitions", increasePartitionsStreamHandler)
                .get("/streams/list", listStreamHandler)
                .get("/streams/info", infoStreamHandler)
                .post("/timelines/create", createTimelineHandler)
                .post("/timelines/delete", deleteTimelineHandler)
                .get("/timelines/list", listTimelineHandler)
                .get("/timelines/info", infoTimelineHandler)
                .post("/rules/set", setRuleHandler)
                .get("/rules/list", listRuleHandler)
                .post("/blacklist/add", addBlacklistHandler)
                .post("/blacklist/remove", removeBlacklistHandler)
                .get("/blacklist/list", listBlacklistHandler);

        undertow = Undertow
                .builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
