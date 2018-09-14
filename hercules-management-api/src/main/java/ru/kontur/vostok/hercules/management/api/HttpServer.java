package ru.kontur.vostok.hercules.management.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.management.api.blacklist.AddBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.ListBlacklistHandler;
import ru.kontur.vostok.hercules.management.api.blacklist.RemoveBlacklistHandler;
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
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.authorization.AdminAuthManagerWrapper;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class HttpServer {
    private final Undertow undertow;

    public HttpServer(
            Properties properties,
            AdminAuthManager adminAuthManager,
            AuthManager authManager,
            StreamRepository streamRepository,
            TimelineRepository timelineRepository,
            BlacklistRepository blacklistRepository,
            RuleRepository ruleRepository,
            CassandraTaskQueue cassandraTaskQueue,
            KafkaTaskQueue kafkaTaskQueue
    ) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesExtractor.get(properties, "port", 6309);

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

        HttpHandler handler = Handlers.routing()
                .get("/ping", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.endExchange();
                })
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
