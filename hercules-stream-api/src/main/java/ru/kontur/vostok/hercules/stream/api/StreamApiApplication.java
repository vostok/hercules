package ru.kontur.vostok.hercules.stream.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Map;
import java.util.Properties;


public class StreamApiApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamApiApplication.class);

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static StreamReader streamReader;
    private static AuthManager authManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            streamReader = new StreamReader(consumerProperties, new StreamRepository(curatorClient));

            authManager = new AuthManager(curatorClient);

            server = new HttpServer(httpServerProperties, authManager, new ReadStreamHandler(streamReader));
            server.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting stream API", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(StreamApiApplication::shutdown));

        LOGGER.info("Stream API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Stream API shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping server");
            //TODO: Process error
        }
        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator client", t);
            //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping auth manager", t);
        }

        try {
            if (streamReader != null) {
                streamReader.stop();
            }
        }
        catch (Throwable t) {
            LOGGER.error("Error on stopping stream reader", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Stream API shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
