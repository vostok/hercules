package ru.kontur.vostok.hercules.meta.sink.sentry;

import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.util.Objects;

/**
 * SentryProjectMappingRecordSerializer
 *
 * @author Kirill Sulim
 */
public class SentryProjectMappingRecordSerializer {

    /**
     * Deserialize String to record for sentry registry
     *
     * @param record Record as String
     * @return Record as SentryProjectMappingRecord
     */
    static SentryProjectMappingRecord deserialize(final String record) {
        String[] split = record.split(":");
        if (split.length != 4) {
            throw new IllegalArgumentException(String.format("Invalid sentry registry record: '%s'", record));
        }

        final String project = StringUtil.requireNotEmpty(split[0]);
        final String service = StringUtil.emptyToNull(split[1]);
        final String sentryOrganization = StringUtil.requireNotEmpty(split[2]);
        final String sentryProject = StringUtil.requireNotEmpty(split[3]);

        return new SentryProjectMappingRecord(
            project,
            service,
            sentryOrganization,
            sentryProject
        );
    }

    /**
     * Serialize record from sentry registry to String
     *
     * @param record Record as SentryProjectMappingRecord
     * @return Record as string
     */
    static String serialize(final SentryProjectMappingRecord record) {
        final String project = Objects.requireNonNull(record.getProject());
        final String service = StringUtil.nullToEmpty(record.getService());
        final String sentryOrganization = Objects.requireNonNull(record.getSentryOrganization());
        final String sentryProject = Objects.requireNonNull(record.getSentryProject());

        return String.join(":", project, service, sentryOrganization, sentryProject);
    }
}
