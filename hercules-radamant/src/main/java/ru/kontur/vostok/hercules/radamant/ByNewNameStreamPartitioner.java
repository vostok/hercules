package ru.kontur.vostok.hercules.radamant;

import java.util.UUID;
import org.apache.kafka.streams.processor.StreamPartitioner;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.tags.MetricsTags;

/**
 * @author Tatyana Tokmyanina
 */
public class ByNewNameStreamPartitioner implements StreamPartitioner<UUID, Event> {
    @Override
    public Integer partition(String topic, UUID key, Event value, int numPartitions) {
        Container enrichmentContainer = (Container) value.getPayload().tags()
                .get(MetricsTags.ENRICHMENT_TAG.getName()).getValue();
        String newName = new String((byte[]) enrichmentContainer.get(TinyString.of("new_name")).getValue());
        return newName.hashCode() % numPartitions;
    }
}

