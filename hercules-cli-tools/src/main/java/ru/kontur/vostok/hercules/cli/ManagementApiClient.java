package ru.kontur.vostok.hercules.cli;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.Map;
import java.util.Properties;

public class ManagementApiClient {

    private static String server;

    public static void main(String[] args) throws Exception {

        Map<String, String> parameters = ArgsParser.parse(args);
        Properties properties = PropertiesUtil.readProperties(parameters.getOrDefault("management-api-client.properties", "management-api-client.properties"));

        init(properties);


        BaseStream stream = new BaseStream();
        stream.setName("test_elastic");
        stream.setPartitions(1);
        stream.setShardingKey(new String[0]);

        createStream(stream);

        Unirest.shutdown();
    }

    private static void createStream(Stream stream) throws Exception {
        HttpResponse<String> response = Unirest.post(server + "/streams/create")
                .header("apiKey", "test")
                .body(stream)
                .asString();

        System.out.println(response.getStatusText());
    }

    private static void init(Properties properties) {
        server = "http://" + properties.getProperty("server");

        Unirest.setObjectMapper(new ObjectMapper() {

            private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            @Override
            public <T> T readValue(String value, Class<T> valueType) {
                return ThrowableUtil.toUnchecked(() -> objectMapper.readValue(value, valueType));
            }

            @Override
            public String writeValue(Object value) {
                return ThrowableUtil.toUnchecked(() -> objectMapper.writeValueAsString(value));
            }
        });
    }
}
