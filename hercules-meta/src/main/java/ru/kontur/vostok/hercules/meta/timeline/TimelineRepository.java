package ru.kontur.vostok.hercules.meta.timeline;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
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
public class TimelineRepository {
    private final CuratorClient curatorClient;
    private final Deserializer deserializer;
    private final Serializer serializer;

    public TimelineRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;

        this.deserializer = Deserializer.forClass(Timeline.class);
        this.serializer = Serializer.forClass(Timeline.class);
    }

    public Optional<Timeline> read(String name) throws CuratorException, DeserializationException {
        ReadResult readResult = curatorClient.read(zPrefix + '/' + name);
        Optional<byte[]> jsonBytes = readResult.getData();
        return jsonBytes.isPresent() ? Optional.of(deserializer.deserialize(jsonBytes.get())) : Optional.empty();
    }

    public CreationResult create(Timeline timeline) throws CuratorException, SerializationException {
        return curatorClient.create(zPrefix + '/' + timeline.getName(), serializer.serialize(timeline));
    }

    public UpdateResult update(Timeline timeline) throws CuratorException, SerializationException {
        return curatorClient.update(zPrefix + '/' + timeline.getName(), serializer.serialize(timeline));
    }

    public List<String> list() throws CuratorException {
        return curatorClient.children(zPrefix);
    }

    public DeletionResult delete(String name) throws CuratorException {
        return curatorClient.delete(zPrefix + '/' + name);
    }

    public boolean exists(String name) throws CuratorException {
        return curatorClient.exists(zPrefix + '/' + name);
    }

    private static String zPrefix = "/hercules/timelines";
}
