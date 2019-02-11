package ru.kontur.vostok.hercules.meta.sink.sentry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SentryProjectRepository
 *
 * @author Kirill Sulim
 */
public class SentryProjectRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryProjectRepository.class);

    private final CuratorClient curatorClient;

    public SentryProjectRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public List<SentryProjectMappingRecord> list() throws Exception {
        return curatorClient.children(zPrefix).stream()
            .map(s -> {
                try {
                    return Optional.of(SentryProjectMappingRecordSerializer.deserialize(s));
                } catch (Exception e) {
                    LOGGER.warn("Exception on deserialization of sentry project mapping string '{}'", s, e);
                    return Optional.<SentryProjectMappingRecord>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public void add(final SentryProjectMappingRecord record) throws Exception {
        delete(record.getProject(), record.getService());
        curatorClient.createIfAbsent(zPrefix + "/" + SentryProjectMappingRecordSerializer.serialize(record));
    }

    public void delete(@NotNull final String project, @Nullable final String service) throws Exception {
        final String search = project + ":" + StringUtil.nullToEmpty(service ) + ":";
        curatorClient.children(zPrefix).stream()
                .filter(record -> record.startsWith(search))
                .forEach(record -> ThrowableUtil.toUnchecked(() -> curatorClient.delete(zPrefix + "/" + record)));
    }

    private static String zPrefix = "/hercules/sink/sentry/registry";
}
