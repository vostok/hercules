package ru.kontur.vostok.hercules.json.mapping;

import ru.kontur.vostok.hercules.json.DocumentWriter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HTree;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Maps {@link Event} to the JSON-document and write it to {@link OutputStream}.
 * <p>
 * Features:
 * <ul>
 *   <li>Event timestamp can be used as JSON-document field,
 *   <li>Event payload can be mapped using {@link Mapper} implementations,
 *   <li>By default, {@link Mapper#PLAIN} is used if no mapping is specified.
 * </ul>
 *
 * @author Gregory Koshelev
 * @see MappingLoader
 * @see Mapper
 * @see DocumentWriter
 */
public class EventMappingWriter {
    private final boolean timestampEnabled;
    private final String timestampField;
    private final DateTimeFormatter timestampFormatter;
    private final HTree<Mapper> mappers;

    public EventMappingWriter(Properties properties) {
        timestampEnabled = PropertiesUtil.get(Props.TIMESTAMP_ENABLE, properties).get();
        if (timestampEnabled) {
            timestampField = PropertiesUtil.get(Props.TIMESTAMP_FIELD, properties).get();
            timestampFormatter = DateTimeFormatter.ofPattern(PropertiesUtil.get(Props.TIMESTAMP_FORMAT, properties).get()).withZone(ZoneOffset.UTC);
        } else {
            timestampField = null;
            timestampFormatter = null;
        }

        mappers = MappingLoader.loadMapping(PropertiesUtil.get(Props.FILE, properties).get());
    }

    public void write(OutputStream out, Event event) throws IOException {
        Map<String, Object> document = new LinkedHashMap<>();

        if (timestampEnabled) {
            document.put(timestampField, timestampFormatter.format(TimeUtil.unixTicksToInstant(event.getTimestamp())));
        }

        HTree<Mapper>.Navigator navigator = mappers.navigator();
        process(document, navigator, event.getPayload());

        DocumentWriter.writeTo(out, document);
    }

    private void process(Map<String, Object> document, HTree<Mapper>.Navigator navigator, Container container) {
        for (Map.Entry<TinyString, Variant> tag : container.tags().entrySet()) {
            Variant value = tag.getValue();
            if (navigator.navigateToChild(tag.getKey())) {
                Mapper mapper = navigator.getValue();
                if (mapper != null) {
                    mapper.map(tag.getKey(), value, document);
                } else {
                    if (value.getType() != Type.CONTAINER || !navigator.hasChildren()) {
                        Mapper.PLAIN.map(tag.getKey(), value, document);
                    } else {
                        process(document, navigator, (Container) value.getValue());
                    }
                }
                navigator.navigateToParent();
            } else {
                Mapper.PLAIN.map(tag.getKey(), value, document);
            }
        }
    }

    static class Props {
        static final Parameter<Boolean> TIMESTAMP_ENABLE =
                Parameter.booleanParameter("timestamp.enable").
                        withDefault(true).
                        build();

        static final Parameter<String> TIMESTAMP_FIELD =
                Parameter.stringParameter("timestamp.field").
                        withDefault("@timestamp").
                        build();

        static final Parameter<String> TIMESTAMP_FORMAT =
                Parameter.stringParameter("timestamp.format").
                        withDefault("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX").
                        build();

        static final Parameter<String> FILE =
                Parameter.stringParameter("file").
                        required().
                        build();
    }
}
