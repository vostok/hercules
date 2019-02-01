package ru.kontur.vostok.hercules.meta.stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import ru.kontur.vostok.hercules.meta.curator.CreationResult;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.curator.DeletionResult;
import ru.kontur.vostok.hercules.meta.curator.UpdateResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamRepository {
    private final CuratorClient curatorClient;
    private final ObjectReader deserializer;
    private final ObjectWriter serializer;

    public StreamRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        ObjectMapper objectMapper = new ObjectMapper().
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.deserializer = objectMapper.readerFor(Stream.class);
        this.serializer = objectMapper.writerFor(Stream.class);
    }

    public Optional<Stream> read(String name) throws Exception {
        Optional<byte[]> jsonBytes = curatorClient.read(zPrefix + '/' + name);
        return jsonBytes.isPresent() ? Optional.of(deserializer.readValue(jsonBytes.get())) : Optional.empty();
    }

    public CreationResult create(Stream stream) throws Exception {
        return curatorClient.create(zPrefix + '/' + stream.getName(), serializer.writeValueAsBytes(stream));
    }

    public List<String> list() throws Exception {
        return curatorClient.children(zPrefix);
    }

    public DeletionResult delete(String name) throws Exception {
        return curatorClient.delete(zPrefix + '/' + name);
    }

    public UpdateResult update(Stream stream) throws Exception {
        return curatorClient.update(zPrefix + '/' + stream.getName(), serializer.writeValueAsBytes(stream));
    }

    private static String zPrefix = "/hercules/streams";
}
