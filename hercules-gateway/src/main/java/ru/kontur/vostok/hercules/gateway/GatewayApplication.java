package ru.kontur.vostok.hercules.gateway;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.gateway.args.ArgsParser;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GatewayApplication {
    private static Undertow undertow;
    private static EventSender eventSender;

    public static void main(String[] args) {
        Map<String, String> parameters = ArgsParser.parse(args);

        Properties producerProperties = readProperties(parameters.getOrDefault("producer.properties", "producer.properties"));

        eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

        StreamRepository streamRepository = new StreamRepository();

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
    }

    private static Properties readProperties(String path) {
        Properties properties = new Properties();
        try(InputStream in = new FileInputStream(path)) {
            properties.load(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }
}
