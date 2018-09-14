package ru.kontur.vostok.hercules.meta.sink.sentry;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;

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

    public List<String> list() throws Exception {
        List<String> rules = curatorClient.children(zPrefix);
        return rules;
    }

    public void assign(String project, String sentryProjectId) throws Exception {
        delete(project);
        String record = project + ":" + sentryProjectId.replaceAll("/", ":");
        curatorClient.createIfAbsent(zPrefix + "/" + record);
    }

    public void delete(String project) throws Exception {
        List<String> projectRecords = list().stream()
                .filter(record -> record.startsWith(project + ":"))
                .collect(Collectors.toList());

        for (String projectRecord : projectRecords) {
            curatorClient.delete(projectRecord);
        }
    }

    private static String zPrefix = "/hercules/sink/sentry/registry";
}
