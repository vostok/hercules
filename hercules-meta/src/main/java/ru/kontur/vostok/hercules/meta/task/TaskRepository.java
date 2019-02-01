package ru.kontur.vostok.hercules.meta.task;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.zookeeper.CreateMode;
import ru.kontur.vostok.hercules.meta.curator.CreationResult;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.curator.DeletionResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class TaskRepository<T> {
    private final CuratorClient curatorClient;
    private final ObjectReader deserializer;
    private final ObjectWriter serializer;
    private final String zPrefix;

    protected TaskRepository(CuratorClient curatorClient, Class<T> clazz, String zPrefix) {
        this.curatorClient = curatorClient;

        ObjectMapper objectMapper = new ObjectMapper().
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.deserializer = objectMapper.readerFor(clazz);
        this.serializer = objectMapper.writerFor(clazz);

        this.zPrefix = zPrefix;
    }

    /**
     * Read task by full task name which is {@code name + '.' + sequence_id}
     * @param fullName
     * @return task if it exists or @{code Optional.empty()} otherwise
     * @throws Exception
     */
    public Optional<T> read(String fullName) throws Exception {
        Optional<byte[]> jsonBytes = curatorClient.read(zPrefix + '/' + fullName);
        return jsonBytes.isPresent() ? Optional.of(deserializer.readValue(jsonBytes.get())) : Optional.empty();
    }

    /**
     * Create task with full name which is {@code name + '.' + sequence_id}, where {@code sequence_id} is auto-generated sequence number
     * @param task to be created
     * @return result of creation
     * @throws Exception
     */
    public CreationResult create(T task, String name) throws Exception {
        return curatorClient.createWithMode(
                zPrefix + '/' + name + TaskConstants.SEQUENCE_DELIMITER,
                serializer.writeValueAsBytes(task),
                CreateMode.PERSISTENT_SEQUENTIAL);
    }

    public List<String> list() throws Exception {
        return curatorClient.children(zPrefix);
    }

    public DeletionResult delete(String fullName) {
        return curatorClient.delete(zPrefix + '/' + fullName);
    }
}
