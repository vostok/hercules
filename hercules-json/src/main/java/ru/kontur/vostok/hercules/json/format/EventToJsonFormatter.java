package ru.kontur.vostok.hercules.json.format;

import ru.kontur.vostok.hercules.json.Document;
import ru.kontur.vostok.hercules.json.format.transformer.Transformer;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HTree;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Format {@link Event} to the JSON-document.
 * <p>
 * Features:
 * <ul>
 *   <li>Event timestamp can be used as JSON-document field,
 *   <li>Event payload can be mapped using {@link Mapper} implementations,
 *   <li>If no mapping is specified for some tags then {@link Transformer#PLAIN} is used by default
 *   or you can ignore these tags.
 * </ul>
 *
 * @author Gregory Koshelev
 * @see MappingLoader
 * @see Mapper
 */
public class EventToJsonFormatter {
    private final boolean timestampEnabled;
    private final String timestampField;
    private final DateTimeFormatter timestampFormatter;
    private final Mapping mapping;
    private final boolean ignoreUnknownTags;

    public EventToJsonFormatter(Properties properties) {
        timestampEnabled = PropertiesUtil.get(Props.TIMESTAMP_ENABLE, properties).get();
        if (timestampEnabled) {
            timestampField = PropertiesUtil.get(Props.TIMESTAMP_FIELD, properties).get();
            timestampFormatter = DateTimeFormatter.ofPattern(PropertiesUtil.get(Props.TIMESTAMP_FORMAT, properties).get()).withZone(ZoneOffset.UTC);
        } else {
            timestampField = null;
            timestampFormatter = null;
        }

        mapping = MappingLoader.loadMapping(PropertiesUtil.get(Props.FILE, properties).get());
        ignoreUnknownTags = PropertiesUtil.get(Props.IGNORE_UNKNOWN_TAGS, properties).get();
    }

    public Document format(Event event) {
        Document document = new Document();

        if (timestampEnabled) {
            document.putIfAbsent(timestampField, timestampFormatter.format(TimeUtil.unixTicksToInstant(event.getTimestamp())));
        }

        for (Iterator<Mapper> it = mapping.iterator(); it.hasNext(); ) {
            Mapper mapper = it.next();
            mapper.map(event, document);
        }

        if (!ignoreUnknownTags) {
            HTree<Boolean>.Navigator navigator = mapping.navigator();
            process(document, navigator, event.getPayload());
        }

        return document;
    }

    private void process(Document document, HTree<Boolean>.Navigator navigator, Container container) {
        for (Map.Entry<TinyString, Variant> tag : container.tags().entrySet()) {
            Variant value = tag.getValue();
            if (navigator.navigateToChild(tag.getKey())) {
                if (!navigator.hasValue()) {
                    if (value.getType() != Type.CONTAINER || !navigator.hasChildren()) {
                        document.putIfAbsent(tag.getKey().toString(), Transformer.PLAIN.transform(tag.getValue()));
                    } else {
                        process(document, navigator, (Container) value.getValue());
                    }
                }
                navigator.navigateToParent();
            } else {
                document.putIfAbsent(tag.getKey().toString(), Transformer.PLAIN.transform(tag.getValue()));
            }
        }
    }

    public static class Props {
        public static final Parameter<Boolean> TIMESTAMP_ENABLE =
                Parameter.booleanParameter("timestamp.enable").
                        withDefault(true).
                        build();

        public static final Parameter<String> TIMESTAMP_FIELD =
                Parameter.stringParameter("timestamp.field").
                        withDefault("@timestamp").
                        build();

        public static final Parameter<String> TIMESTAMP_FORMAT =
                Parameter.stringParameter("timestamp.format").
                        withDefault("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX").
                        build();

        public static final Parameter<String> FILE =
                Parameter.stringParameter("file").
                        required().
                        build();

        public static final Parameter<Boolean> IGNORE_UNKNOWN_TAGS =
                Parameter.booleanParameter("ignore.unknown.tags").
                        withDefault(false).
                        build();
    }
}
