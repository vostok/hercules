package ru.kontur.vostok.hercules.elastic.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class IndexToElasticJsonWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexToElasticJsonWriter.class);

    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static final byte[] START_BYTES = "{\"index\":{\"_index\":\"".getBytes(ENCODING);
    private static final byte[] MIDDLE_BYTES = "\",\"_type\":\"LogEvent\",\"_id\":\"".getBytes(ENCODING);
    private static final byte[] END_BYTES = "\"}}".getBytes(ENCODING);

    public static boolean tryWriteIndex(OutputStream stream, Event event) throws IOException {
        Optional<String> index;
        try {
            index = extractIndex(event);
        } catch (Exception ex) {
            LOGGER.warn("Cannot extract index from event", ex);
            return false;
        }
        if (!index.isPresent()) {
            return false;
        }

        stream.write(START_BYTES);
        stream.write(index.get().getBytes(ENCODING));
        stream.write(MIDDLE_BYTES);
        stream.write(EventUtil.extractStringId(event).getBytes(ENCODING));
        stream.write(END_BYTES);
        return true;
    }

    private static Optional<String> extractIndex(final Event event) {
        return ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG)
                .flatMap(properties -> {
                    final List<String> parts = new ArrayList<>(4);

                    final Optional<String> index = ContainerUtil.extract(properties, ElasticSearchTags.ELK_INDEX_TAG);
                    if (index.isPresent()) {
                        parts.add(index.get());
                    } else {
                        final Optional<String> project = ContainerUtil.extract(properties, CommonTags.PROJECT_TAG);
                        if (project.isPresent()) {
                            parts.add(project.get());
                            Optional<String> application = ContainerUtil.extract(properties, CommonTags.APPLICATION_TAG);
                            application.ifPresent(parts::add);
                            if (!application.isPresent()) {//FIXME: backward compatibility
                                ContainerUtil.extract(properties, ElasticSearchTags.ELK_SCOPE_TAG).ifPresent(parts::add);
                            }
                            ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG).ifPresent(parts::add);
                        }
                    }

                    if (parts.size() != 0) {
                        parts.add(DATE_FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp())));
                        return Optional.of(String.join("-", parts).toLowerCase());
                    }  else {
                        return Optional.empty();
                    }
                });
    }

    private IndexToElasticJsonWriter() {
        /* static class */
    }
}
