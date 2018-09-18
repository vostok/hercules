package ru.kontur.vostok.hercules.meta.sink.sentry;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SentryProjectRepository
 *
 * @author Kirill Sulim
 */
public class SentryProjectRepository {

    private final CuratorClient curatorClient;

    public SentryProjectRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public List<SentryProjectMappingRecord> list() throws Exception {
        return curatorClient.children(zPrefix).stream()
                .map(SentryProjectRepository::deserialize)
                .collect(Collectors.toList());
    }

    public void add(SentryProjectMappingRecord record) throws Exception {
        delete(record.getProject());
        curatorClient.createIfAbsent(zPrefix + "/" + serialize(record));
    }

    public void delete(String project) throws Exception {
        final String search = project + ":";
        curatorClient.children(zPrefix).stream()
                .filter(record -> record.startsWith(search))
                .forEach(record -> ThrowableUtil.toUnchecked(() -> curatorClient.delete(zPrefix + "/" + record)));
    }

    private static SentryProjectMappingRecord deserialize(String record) {
        String[] split = record.split(":");
        if (split.length != 3) {
            throw new IllegalArgumentException(String.format("Invalid sentry registry record: '%s'", record));
        }
        return new SentryProjectMappingRecord(split[0], split[1] + "/" + split[2]);
    }

    private static String serialize(SentryProjectMappingRecord record) {
        String[] split = record.getSentryProject().split("/");
        if (split.length != 2) {
            throw new IllegalArgumentException(String.format("Invalid sentry project name: '%s'", record.getSentryProject()));
        }
        return record.getProject() + ":" + split[0] + ":" + split[1];
    }

    private static String zPrefix = "/hercules/sink/sentry/registry";
}
