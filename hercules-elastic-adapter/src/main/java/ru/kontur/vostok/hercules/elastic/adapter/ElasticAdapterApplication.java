package ru.kontur.vostok.hercules.elastic.adapter;

import ru.kontur.vostok.hercules.gateway.client.GatewayClient;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class ElasticAdapterApplication {
    private final HttpServer httpServer;
    private final GatewayClient gatewayClient;

    //TODO: do trim operation
    public ElasticAdapterApplication(Properties properties) {
        if (!properties.containsKey("url") || !properties.containsKey("apiKey") || !properties.containsKey("stream")) {
            throw new IllegalStateException();
        }

        String url = properties.getProperty("url");
        String apiKey = properties.getProperty("apiKey");

        gatewayClient = new GatewayClient(url, apiKey);

        int port = PropertiesUtil.get(properties, "port", 6307);
        String host = properties.getProperty("host", "0.0.0.0");
        String stream = properties.getProperty("stream");

        httpServer = new HttpServer(host, port, stream, gatewayClient);

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
        gatewayClient.close();
    }
}
