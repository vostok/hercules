package ru.kontur.vostok.hercules.elastic.adapter.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexMeta;
import ru.kontur.vostok.hercules.elastic.adapter.index.TimestampFormat;
import ru.kontur.vostok.hercules.elastic.adapter.format.mapping.Mapper;
import ru.kontur.vostok.hercules.elastic.adapter.format.mapping.Mapping;
import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map;

/**
 * Format the JSON-document to {@link Event}.
 * <p>
 * Features:
 * <ul>
 *   <li>Uses {@code timestamp} field in the JSON-document as timestamp for the event. Timestamp format is defined for the index in the index meta.</li>
 *   <li>Also, generates timestamp if {@code @timestamp} field is absent or has invalid format.</li>
 *   <li>Uses optional properties from the index meta to enrich the event.</li>
 *   <li>Adds the index name to the event.</li>
 *   <li>Uses configured list of {@link Mapper} to construct the event.</li>
 * </ul>
 *
 * @author Gregory Koshelev
 */
public class JsonToEventFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToEventFormatter.class);

    private static final String TIMESTAMP_FIELD = "@timestamp";//TODO: Move to configuration

    /**
     * Format the JSON-document to the {@link Event} using the index meta.
     *
     * @param document  the JSON-document
     * @param index     the index name
     * @param indexMeta the index meta
     * @return the event
     */
    public static Event format(Document document, String index, IndexMeta indexMeta) {
        Mapping mapping = indexMeta.getMapping();
        ProtoContainer proto = new ProtoContainer();

        for (Iterator<Mapper> it = mapping.iterator(); it.hasNext(); ) {
            Mapper mapper = it.next();
            mapper.map(document, proto);
        }

        for (Map.Entry<HPath, Variant> property : indexMeta.getProperties().entrySet()) {
            proto.put(property.getKey(), property.getValue());
        }

        if (!HPath.isNullOrEmpty(indexMeta.getIndexPath())) {
            proto.put(indexMeta.getIndexPath(), Variant.ofString(index));
        }

        EventBuilder eventBuilder = EventBuilder.create().
                version(1).
                timestamp(extractTimestamp(document.get(TIMESTAMP_FIELD), indexMeta.getTimestampFormat())).
                uuid(UuidGenerator.getClientInstance().next());

        proto.fill(eventBuilder);

        return eventBuilder.build();
    }

    /**
     * Extract the timestamp from the raw value or take current timestamp.
     *
     * @param value  the raw value
     * @param format the format of the timestamp
     * @return Hercules compatible timestamp
     */
    private static long extractTimestamp(Object value, TimestampFormat format) {
        if (value == null) {
            return defaultTimestamp();
        }

        switch (format) {
            case ISO_DATE_TIME:
                if (!(value instanceof String)) {
                    return defaultTimestamp();
                }
                final ZonedDateTime zonedDateTime;
                try {
                    zonedDateTime = (ZonedDateTime) DateTimeFormatter.ISO_DATE_TIME.parseBest(
                            (String) value,
                            ZonedDateTime::from,
                            temporal -> LocalDateTime.from(temporal).atZone(ZoneId.of("UTC")));
                } catch (DateTimeParseException ex) {
                    LOGGER.warn("Invalid date format", ex);
                    return defaultTimestamp();
                }
                return TimeUtil.dateTimeToUnixTicks(zonedDateTime);
            case UNIX_TIME:
                if (!(value instanceof Long)) {
                    return defaultTimestamp();
                }
                return TimeUtil.unixTimeToUnixTicks((Long) value);
            default:
                return defaultTimestamp();
        }
    }

    private static long defaultTimestamp() {
        return TimeUtil.millisToTicks(TimeSource.SYSTEM.milliseconds());
    }
}
