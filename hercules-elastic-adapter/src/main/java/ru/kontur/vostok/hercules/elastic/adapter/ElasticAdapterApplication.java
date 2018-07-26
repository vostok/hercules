package ru.kontur.vostok.hercules.elastic.adapter;

import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

/**
 * The point to launch the Elastic Adapter.
 * Need .properties file to start application. Required fields are <code>url</code> and <code>stream</code>
 *
 * <code>url</code> - url where events are sent
 * <code>stream</code> - topic in kafka where data is sent
 *
 *  Example of start:
 *  <code>java -jar elastic-adapter.jar elastic.properties=elastic.properties</code>
 *
 * @author Daniil Zhenikhov
 */
public class ElasticAdapterApplication {
    private static final int DEFAULT_PORT = 6307;
    private static final String DEFAULT_HOST = "0.0.0.0";

    private static final String URL = "url";
    private static final String STREAM = "stream";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String API_KEY = "apiKey";

    private final HttpServer httpServer;

    public ElasticAdapterApplication(Properties properties) {
        String url = PropertiesUtil
                .getAs(properties,URL, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(URL));
        String stream = PropertiesUtil
                .getAs(properties, STREAM, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(STREAM));
        String apiKey = PropertiesUtil
                .getAs(properties, API_KEY, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(API_KEY));
        int port = PropertiesUtil
                .getAs(properties, PORT, Integer.class)
                .orElse(DEFAULT_PORT);
        String host = PropertiesUtil
                .getAs(properties, HOST, String.class)
                .orElse(DEFAULT_HOST);

        httpServer = new HttpServer(host, port, stream, url, apiKey);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public static void main(String[] args) {
        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("elastic.properties", "elastic.properties"));

        ElasticAdapterApplication elasticAdapterApplication = new ElasticAdapterApplication(properties);
        elasticAdapterApplication.start();
    }

    public void start() {
        System.out.println("Elastic Adapter's started");
        httpServer.start();
    }

    private void stop() {
        httpServer.stop();
    }
}
