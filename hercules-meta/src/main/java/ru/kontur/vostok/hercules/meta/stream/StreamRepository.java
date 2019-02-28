package ru.kontur.vostok.hercules.meta.stream;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.meta.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.curator.result.DeletionResult;
import ru.kontur.vostok.hercules.meta.curator.result.ReadResult;
import ru.kontur.vostok.hercules.meta.curator.result.UpdateResult;
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
    private final CuratorClient curatorClient;
    private final Deserializer deserializer;
    private final Serializer serializer;

    public StreamRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.deserializer = Deserializer.forClass(Stream.class);
        this.serializer = Serializer.forClass(Stream.class);
    }

    public Optional<Stream> read(String name) throws CuratorUnknownException, CuratorInternalException, DeserializationException {
        ReadResult readResult = curatorClient.read(zPrefix + '/' + name);
        Optional<byte[]> jsonBytes = readResult.getData();
        return jsonBytes.isPresent() ? Optional.of(deserializer.deserialize(jsonBytes.get())) : Optional.empty();
    }

    public CreationResult create(Stream stream) throws SerializationException, CuratorUnknownException, CuratorInternalException {
        return curatorClient.create(zPrefix + '/' + stream.getName(), serializer.serialize(stream));
    }

    public List<String> list() throws Exception {
        return curatorClient.children(zPrefix);
    }

    public DeletionResult delete(String name) throws CuratorUnknownException, CuratorInternalException {
        return curatorClient.delete(zPrefix + '/' + name);
    }

    public UpdateResult update(Stream stream) throws SerializationException, CuratorUnknownException, CuratorInternalException {
        return curatorClient.update(zPrefix + '/' + stream.getName(), serializer.serialize(stream));
    }

    public boolean exists(String name) throws CuratorUnknownException {
        return curatorClient.exists(zPrefix + '/' + name);
    }

    private static String zPrefix = "/hercules/streams";
}
