package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.format.EventFormatter;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.ZonedDateTime;

final class EventToLeproseryEvent {

    private static final String PROJECT_NAME = "hercules";
    private static final String SERVICE_NAME = "hercules-elastic-sink";

    /**
     * Create Hercules Protocol event for leprosery with json line of invalid event
     *
     * @param event  non-retryable event
     * @param leproseryIndex name of destination index
     * @param index  to be used when put events to ELK
     * @param reason message of error
     * @return Hercules Protocol event which is sent to leprosery
     */
    static Event toLeproseryEvent(Event event, String leproseryIndex, String index, String reason) {
        return EventBuilder.create()
                .version(1)
                .timestamp(TimeUtil.dateTimeToUnixTicks(ZonedDateTime.now()))
                .random(UuidGenerator.getClientInstance().next())
                .tag("message", Variant.ofString(reason))
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(PROJECT_NAME))
                        .tag(CommonTags.SERVICE_TAG, Variant.ofString(SERVICE_NAME))
                        .tag("text", Variant.ofString(EventFormatter.format(event, true)))
                        .tag("original-index", Variant.ofString(index))
                        .tag(ElasticSearchTags.ELK_INDEX_TAG.getName(), Variant.ofString(leproseryIndex))
                        .build()))
                .build();
    }

}
