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
    private static final String URL = "url";
    private static final String STREAM = "stream";
    private static final String HOST = "host";
    private static final String PORT = "port";

    private final HttpServer httpServer;

    //TODO: do trim operation
    public ElasticAdapterApplication(Properties properties) {
        if (!properties.containsKey("url") || !properties.containsKey("stream")) {
            throw new IllegalArgumentException("Missing required property ('url', 'stream')");
        }

        String url = PropertiesUtil
                .getAs(properties,"url", String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(URL));
        String stream = PropertiesUtil
                .getAs(properties, "stream", String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(STREAM));

        int port = PropertiesUtil.get(properties, "port", 6307);
        String host = properties.getProperty("host", "0.0.0.0");

        httpServer = new HttpServer(host, port, stream, url);

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
