package ru.kontur.vostok.hercules.meta.sink.sentry;

import org.junit.Test;

import static org.junit.Assert.*;

public class SentryProjectMappingRecordSerializerTest {

    @Test
    public void shouldSerializeProjectWithoutService() throws Exception {

        final SentryProjectMappingRecord record = new SentryProjectMappingRecord(
            "project",
            null,
            "org-sentry",
            "project-sentry"
        );

        final String serialized = SentryProjectMappingRecordSerializer.serialize(record);

        assertEquals("project::org-sentry:project-sentry", serialized);
    }

    @Test
    public void shouldDeserializeProjectWithoutService() throws Exception {

        final SentryProjectMappingRecord record = SentryProjectMappingRecordSerializer.deserialize("project::org-sentry:project-sentry");

        assertEquals("project", record.getProject());
        assertEquals(null, record.getService());
        assertEquals("org-sentry", record.getSentryOrganization());
        assertEquals("project-sentry", record.getSentryProject());
    }

    @Test
    public void shouldSerializeProjectWithService() throws Exception {

        final SentryProjectMappingRecord record = new SentryProjectMappingRecord(
            "project",
            "service",
            "org-sentry",
            "project-sentry"
        );

        final String serialized = SentryProjectMappingRecordSerializer.serialize(record);

        assertEquals("project:service:org-sentry:project-sentry", serialized);

    }

    @Test
    public void shouldDeserializeProjectWithService() throws Exception {

        final SentryProjectMappingRecord record = SentryProjectMappingRecordSerializer.deserialize("project:service:org-sentry:project-sentry");

        assertEquals("project", record.getProject());
        assertEquals("service", record.getService());
        assertEquals("org-sentry", record.getSentryOrganization());
        assertEquals("project-sentry", record.getSentryProject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnIncorrectDataOnDeserialization() throws Exception {
        SentryProjectMappingRecordSerializer.deserialize(":::");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionOnIncorrectDataOnSerialization() throws Exception {
        SentryProjectMappingRecordSerializer.serialize(new SentryProjectMappingRecord(null, null, null, null));
    }
}
