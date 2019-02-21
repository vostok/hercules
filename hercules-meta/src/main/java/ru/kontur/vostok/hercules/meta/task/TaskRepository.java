package ru.kontur.vostok.hercules.meta.task;

import org.apache.zookeeper.CreateMode;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.meta.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.curator.result.DeletionResult;
import ru.kontur.vostok.hercules.meta.curator.result.ReadResult;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.serialization.Deserializer;
import ru.kontur.vostok.hercules.meta.serialization.SerializationException;
import ru.kontur.vostok.hercules.meta.serialization.Serializer;

import java.util.List;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class TaskRepository<T> {
    private final CuratorClient curatorClient;
    private final Deserializer deserializer;
    private final Serializer serializer;
    private final String zPrefix;

    protected TaskRepository(CuratorClient curatorClient, Class<T> clazz, String zPrefix) {
        this.curatorClient = curatorClient;

        this.deserializer = Deserializer.forClass(clazz);
        this.serializer = Serializer.forClass(clazz);

        this.zPrefix = zPrefix;
    }

    /**
     * Read task by full task name which is {@code name + '.' + sequence_id}
     *
     * @param fullName
     * @return task if it exists or @{code Optional.empty()} otherwise
     */
    public Optional<T> read(String fullName) throws CuratorUnknownException, CuratorInternalException, DeserializationException {
        ReadResult readResult = curatorClient.read(zPrefix + '/' + fullName);
        Optional<byte[]> jsonBytes = readResult.getData();

        return jsonBytes.isPresent() ? Optional.of(deserializer.deserialize(jsonBytes.get())) : Optional.empty();
    }

    /**
     * Create task with full name which is {@code name + '.' + sequence_id}, where {@code sequence_id} is auto-generated sequence number
     *
     * @param task to be created
     * @return result of creation
     */
    public CreationResult create(T task, String name) throws SerializationException, CuratorUnknownException, CuratorInternalException {
        return curatorClient.createWithMode(
                zPrefix + '/' + name + TaskConstants.SEQUENCE_DELIMITER,
                serializer.serialize(task),
                CreateMode.PERSISTENT_SEQUENTIAL);
    }

    /**
     * Check task existence by full name
     *
     * @param fullName task's full name
     * @return {@code true} if task exists, {@code false} otherwise
     * @throws CuratorUnknownException
     */
    public boolean exists(String fullName) throws CuratorUnknownException {
        return curatorClient.exists(zPrefix + '/' + fullName);
    }

    public List<String> list() throws Exception {
        return curatorClient.children(zPrefix);
    }

    public DeletionResult delete(String fullName) throws CuratorUnknownException, CuratorInternalException {
        return curatorClient.delete(zPrefix + '/' + fullName);
    }
}
