package ru.kontur.vostok.hercules.meta.stream;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.curator.result.DeletionResult;
import ru.kontur.vostok.hercules.curator.result.ReadResult;
import ru.kontur.vostok.hercules.curator.result.UpdateResult;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.serialization.Deserializer;
import ru.kontur.vostok.hercules.meta.serialization.SerializationException;
import ru.kontur.vostok.hercules.meta.serialization.Serializer;

import java.util.List;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamRepository {
    public static final String Z_PREFIX = "/hercules/streams";

    private final CuratorClient curatorClient;
    private final Deserializer deserializer;
    private final Serializer serializer;

    public StreamRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.deserializer = Deserializer.forClass(Stream.class);
        this.serializer = Serializer.forClass(Stream.class);
    }

    public Optional<Stream> read(String name) throws CuratorException, DeserializationException {
        ReadResult readResult = curatorClient.read(Z_PREFIX + '/' + name);
        Optional<byte[]> jsonBytes = readResult.getData();
        return jsonBytes.isPresent() ? Optional.of(deserializer.deserialize(jsonBytes.get())) : Optional.empty();
    }

    public CreationResult create(Stream stream) throws CuratorException, SerializationException {
        return curatorClient.create(Z_PREFIX + '/' + stream.getName(), serializer.serialize(stream));
    }

    public List<String> list() throws CuratorException {
        return curatorClient.children(Z_PREFIX);
    }

    public DeletionResult delete(String name) throws CuratorException {
        return curatorClient.delete(Z_PREFIX + '/' + name);
    }

    public UpdateResult update(Stream stream) throws CuratorException, SerializationException {
        return curatorClient.update(Z_PREFIX + '/' + stream.getName(), serializer.serialize(stream));
    }

    public boolean exists(String name) throws CuratorException {
        return curatorClient.exists(Z_PREFIX + '/' + name);
    }
}
