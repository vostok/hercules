package ru.kontur.vostok.hercules.management.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.management.api.blacklist.AddBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.ListBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.RemoveBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.sink.sentry.DeleteProjectHandler;
import ru.kontur.vostok.hercules.management.api.sink.sentry.ListProjectHandler;
import ru.kontur.vostok.hercules.management.api.sink.sentry.SetProjectHandler;
import ru.kontur.vostok.hercules.management.api.task.CassandraTaskQueue;
import ru.kontur.vostok.hercules.management.api.task.KafkaTaskQueue;
import ru.kontur.vostok.hercules.management.api.rule.ListRuleHandler;
import ru.kontur.vostok.hercules.management.api.rule.SetRuleHandler;
import ru.kontur.vostok.hercules.management.api.stream.CreateStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.DeleteStreamHandler;
import ru.kontur.vostok.hercules.management.api.stream.ListStreamHandler;
import ru.kontur.vostok.hercules.management.api.timeline.CreateTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.DeleteTimelineHandler;
import ru.kontur.vostok.hercules.management.api.timeline.ListTimelineHandler;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.authorization.AdminAuthManagerWrapper;
import ru.kontur.vostok.hercules.undertow.util.handlers.AboutHandler;
import ru.kontur.vostok.hercules.undertow.util.handlers.PingHandler;
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
            BlacklistRepository blacklistRepository,
            RuleRepository ruleRepository,
            SentryProjectRepository sentryProjectRepository,
            CassandraTaskQueue cassandraTaskQueue,
            KafkaTaskQueue kafkaTaskQueue
    ) {
        final String host = Props.HOST.extract(properties);
        final int port = Props.PORT.extract(properties);

        AdminAuthManagerWrapper adminAuthManagerWrapper = new AdminAuthManagerWrapper(adminAuthManager);

        CreateStreamHandler createStreamHandler = new CreateStreamHandler(authManager, streamRepository, kafkaTaskQueue);
        DeleteStreamHandler deleteStreamHandler = new DeleteStreamHandler(authManager, streamRepository, kafkaTaskQueue);
        ListStreamHandler listStreamHandler = new ListStreamHandler(streamRepository);

        CreateTimelineHandler createTimelineHandler = new CreateTimelineHandler(authManager, timelineRepository, cassandraTaskQueue);
        DeleteTimelineHandler deleteTimelineHandler = new DeleteTimelineHandler(authManager, timelineRepository, cassandraTaskQueue);
        ListTimelineHandler listTimelineHandler = new ListTimelineHandler(timelineRepository);

        HttpHandler setRuleHandler = adminAuthManagerWrapper.wrap(new SetRuleHandler(ruleRepository));
        HttpHandler listRuleHandler = adminAuthManagerWrapper.wrap(new ListRuleHandler(ruleRepository));

        HttpHandler addBlacklistHandler = adminAuthManagerWrapper.wrap(new AddBlacklistHandler(blacklistRepository));
        HttpHandler removeBlacklistHandler = adminAuthManagerWrapper.wrap(new RemoveBlacklistHandler(blacklistRepository));
        HttpHandler listBlacklistHandler = adminAuthManagerWrapper.wrap(new ListBlacklistHandler(blacklistRepository));

        HttpHandler sentryRegistryListHandler = adminAuthManagerWrapper.wrap(new ListProjectHandler(sentryProjectRepository));
        HttpHandler sentryRegistrySetHandler = adminAuthManagerWrapper.wrap(new SetProjectHandler(sentryProjectRepository));
        HttpHandler sentryRegistryDeleteHandler = adminAuthManagerWrapper.wrap(new DeleteProjectHandler(sentryProjectRepository));

        HttpHandler handler = Handlers.routing()
                .get("/ping", PingHandler.INSTANCE)
                .get("/about", AboutHandler.INSTANCE)
                .post("/streams/create", createStreamHandler)
                .post("/streams/delete", deleteStreamHandler)
                .get("/streams/list", listStreamHandler)
                .post("/timelines/create", createTimelineHandler)
                .post("/timelines/delete", deleteTimelineHandler)
                .get("/timelines/list", listTimelineHandler)
                .post("/rules/set", setRuleHandler)
                .get("/rules/list", listRuleHandler)
                .post("/blacklist/add", addBlacklistHandler)
                .post("/blacklist/remove", removeBlacklistHandler)
                .get("/blacklist/list", listBlacklistHandler)
                .get("/sink/sentry/registry", sentryRegistryListHandler)
                .post("/sink/sentry/registry", sentryRegistrySetHandler)
                .delete("/sink/sentry/registry", sentryRegistryDeleteHandler);

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
