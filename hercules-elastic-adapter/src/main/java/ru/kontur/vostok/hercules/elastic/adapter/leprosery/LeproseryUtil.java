package ru.kontur.vostok.hercules.elastic.adapter.leprosery;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.ZonedDateTime;

/**
 * @author Gregory Koshelev
 */
public class LeproseryUtil {
    private static final TinyString MESSAGE_TAG = TinyString.of("message");
    private static final TinyString PROPERTIES_TAG = CommonTags.PROPERTIES_TAG.getName();
    private static final TinyString PROJECT_TAG = CommonTags.PROJECT_TAG.getName();
    private static final TinyString SERVICE_TAG = TinyString.of("service");
    private static final TinyString TEXT_TAG = TinyString.of("text");
    private static final TinyString ORIGINAL_INDEX_TAG = TinyString.of("original-index");
    private static final TinyString ELK_INDEX_TAG = ElasticSearchTags.ELK_INDEX_TAG.getName();

    private static final Variant PROJECT = Variant.ofString("hercules");
    private static final Variant SERVICE = Variant.ofString("elastic-adapter");

    public static Event makeEvent(byte[] originalMessage, String originalIndex, String leproseryIndex, String error) {
        return EventBuilder.create().
                version(1).
                timestamp(TimeUtil.dateTimeToUnixTicks(ZonedDateTime.now())).
                uuid(UuidGenerator.getClientInstance().next()).
                tag(MESSAGE_TAG, Variant.ofString(error)).
                tag(PROPERTIES_TAG,
                        Variant.ofContainer(Container.builder().
                                tag(PROJECT_TAG, PROJECT).
                                tag(SERVICE_TAG, SERVICE).
                                tag(TEXT_TAG, Variant.ofString(originalMessage)).
                                tag(ORIGINAL_INDEX_TAG, Variant.ofString(originalIndex)).
                                tag(ELK_INDEX_TAG, Variant.ofString(leproseryIndex)).
                                build())).
                build();
    }
}
