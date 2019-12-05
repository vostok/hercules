package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
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

    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static final byte[] START_BYTES = "{\"index\":{\"_index\":\"".getBytes(ENCODING);
    private static final byte[] MIDDLE_BYTES = "\",\"_type\":\"LogEvent\",\"_id\":\"".getBytes(ENCODING);
    private static final byte[] END_BYTES = "\"}}".getBytes(ENCODING);

    public static void writeIndex(OutputStream stream, String index, String eventId) throws IOException {
        stream.write(START_BYTES);
        stream.write(index.getBytes(ENCODING));
        stream.write(MIDDLE_BYTES);
        stream.write(eventId.getBytes(ENCODING));
        stream.write(END_BYTES);
    }

    private IndexToElasticJsonWriter() {
        /* static class */
    }
}
