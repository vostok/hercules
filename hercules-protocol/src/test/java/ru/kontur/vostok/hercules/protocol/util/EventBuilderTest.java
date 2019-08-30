package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.UUID;

/**
 * @author Petr Demenev
 */
public class EventBuilderTest {

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnTimestampSettingTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.timestamp(15650016621430000L);
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnRandomSettingTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.random(UUID.randomUUID());
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnVersionSettingTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.version(1);
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnTagSettingTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.tag("some_tag",  Variant.ofString("some_value"));
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnTagSettingWithTagDescriptionTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.tag(
                TagDescriptionBuilder.tag("some_tag", String.class).build(),
                Variant.ofString("some_value"));
    }

    @Test (expected = IllegalStateException.class)
    public void shouldThrowExceptionIfEventWasBuildOnRepeatBuildingTest() {
        EventBuilder eventBuilder = EventBuilder.create().random(UUID.randomUUID());
        eventBuilder.build();

        eventBuilder.build();
    }
}
