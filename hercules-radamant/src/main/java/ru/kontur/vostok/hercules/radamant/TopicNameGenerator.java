package ru.kontur.vostok.hercules.radamant;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.streams.processor.RecordContext;
import org.apache.kafka.streams.processor.TopicNameExtractor;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.tags.MetricsTags;

/**
 * Generate name of topic in which metrics will be redirected.
 *
 * @author Tatyana Tokmyanina
 */
public class TopicNameGenerator implements TopicNameExtractor<UUID, Event> {
    private final String defaultResultStream;

    public TopicNameGenerator(String defaultResultStream) {
        this.defaultResultStream = defaultResultStream;
    }

    @Override
    public String extract(UUID key, Event value, RecordContext recordContext) {
        String result = defaultResultStream;
        if (value.getPayload().tags().containsKey(MetricsTags.ENRICHMENT_TAG.getName())) {
            Variant enrichment = value.getPayload().get(MetricsTags.ENRICHMENT_TAG.getName());
            if (enrichment.getType() != Type.CONTAINER) {
                return result;
            }
            Container container = (Container) enrichment.getValue();
            if (container.tags().containsKey(MetricsTags.TOPIC_NAME_TAG.getName())) {
                Variant topicName = container.get(MetricsTags.TOPIC_NAME_TAG.getName());
                if (topicName.getType() != Type.STRING) {
                    return result;
                }
                result = new String((byte[]) topicName.getValue(), StandardCharsets.UTF_8);
            }
        }
        return result;
    }
}
