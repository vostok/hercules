package ru.kontur.vostok.hercules.meta.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class TimelineRepository {
    private final CuratorClient curatorClient;
    private final ObjectReader deserializer;
    private final ObjectWriter serializer;

    public TimelineRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Timeline.class);
        this.serializer = objectMapper.writerFor(Timeline.class);
    }

    public Optional<Timeline> read(String name) throws Exception {
        Optional<byte[]> jsonBytes = curatorClient.read(zPrefix + name);
        return jsonBytes.isPresent() ? Optional.of(deserializer.readValue(jsonBytes.get())) : Optional.empty();
    }

    private static String zPrefix = "/hercules/timeline/";
}
