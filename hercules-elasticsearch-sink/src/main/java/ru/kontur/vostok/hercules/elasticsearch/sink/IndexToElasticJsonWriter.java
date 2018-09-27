package ru.kontur.vostok.hercules.elasticsearch.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class IndexToElasticJsonWriter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("UTC"));

    private static final byte[] START_BYTES = "{\"index\":{\"_index\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MIDDLE_BYTES = "\",\"_type\":\"LogEvent\",\"_id\":\"".getBytes(StandardCharsets.UTF_8);
    private static final byte[] END_BYTES = "\"}}".getBytes(StandardCharsets.UTF_8);


    public static boolean tryWriteIndex(OutputStream stream, Event event) throws IOException {

        String indexName;
        Optional<String> index = ContainerUtil.extract(event.getPayload(), ElasticSearchTags.INDEX_TAG);
        if (index.isPresent()) {
            indexName = index.get();
        } else {
            Optional<String> project = ContainerUtil.extract(event.getPayload(), CommonTags.PROJECT_TAG);
            Optional<String> env = ContainerUtil.extract(event.getPayload(), CommonTags.ENVIRONMENT_TAG);
            if (project.isPresent() && env.isPresent()) {
                indexName = project.get() + "-" +
                        env.get() + "-" +
                        DATE_FORMATTER.format(TimeUtil.gregorianTicksToInstant(event.getId().timestamp()));
            } else {
                return false;
            }
        }

        stream.write(START_BYTES);
        stream.write(indexName.getBytes(StandardCharsets.UTF_8));
        stream.write(MIDDLE_BYTES);
        stream.write(event.getId().toString().getBytes(StandardCharsets.UTF_8));
        stream.write(END_BYTES);

        return true;
    }

    private IndexToElasticJsonWriter() {
    }
}
