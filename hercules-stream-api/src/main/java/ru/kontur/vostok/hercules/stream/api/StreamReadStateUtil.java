package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.protocol.StreamReadState;
import ru.kontur.vostok.hercules.protocol.StreamShardReadState;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public final class StreamReadStateUtil {
    /**
     * Build {@link StreamReadState} from the Kafka's {@link TopicPartition} to Offset map.
     *
     * @param streamName the name of stream
     * @param map        the map of {@link TopicPartition} to Offset
     * @return read state
     */
    public static StreamReadState stateFromMap(String streamName, Map<TopicPartition, Long> map) {
        return new StreamReadState(
                map.entrySet().stream()
                        .filter(e -> e.getKey().topic().equals(streamName))
                        .map(e -> new StreamShardReadState(e.getKey().partition(), e.getValue()))
                        .collect(Collectors.toList())
                        .toArray(new StreamShardReadState[]{})
        );
    }

    /**
     * Build Kafka's {@link TopicPartition} to Offset map from {@link StreamReadState}.
     *
     * @param streamName the name of {@link ru.kontur.vostok.hercules.meta.stream.Stream}
     * @param state      the {@link StreamReadState}
     * @return map
     */
    public static Map<TopicPartition, Long> stateToMap(String streamName, StreamReadState state) {
        return Arrays.stream(state.getShardStates())
                .collect(Collectors.toMap(
                        shardState -> new TopicPartition(streamName, shardState.getPartition()),
                        StreamShardReadState::getOffset
                ));
    }

    private StreamReadStateUtil() {
        /* static class */
    }
}
