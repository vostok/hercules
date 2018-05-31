package ru.kontur.vostok.hercules.gateway;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GatewayApplication {
    private static Undertow undertow;
    private static EventSender eventSender;
    private static CuratorClient curatorClient;

    public static void main(String[] args) {
        Map<String, String> parameters = ArgsParser.parse(args);

        Properties producerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("producer.properties", "producer.properties"));
        Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));

        eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

        curatorClient = new CuratorClient(curatorProperties);
        curatorClient.start();

        StreamRepository streamRepository = new StreamRepository(curatorClient);

        AuthManager authManager = new AuthManager();
        HttpHandler sendAsyncHandler = new SendAsyncHandler(authManager, eventSender, streamRepository);
        HttpHandler sendHandler = new SendHandler(authManager, eventSender, streamRepository);

        HttpHandler handler = Handlers.routing()
                .get("/ping", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.endExchange();
                })
                .post("/stream/sendAsync", sendAsyncHandler)
                .post("/stream/send", sendHandler);

        undertow = Undertow
                .builder()
                .addHttpListener(6306, "0.0.0.0")
                .setHandler(handler)
                .build();
        undertow.start();

        Runtime.getRuntime().addShutdownHook(new Thread(GatewayApplication::shutdown));
    }

    static void shutdown() {
        try {
            undertow.stop();
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            eventSender.stop(5_000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            curatorClient.stop();
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }
    }
}
