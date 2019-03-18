package ru.kontur.vostok.hercules.protocol.format;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TestUtil;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class EventFormatterTest {

    @Test
    public void shouldPrettyPrintEvent() throws Exception {
        final Event event = EventBuilder.create()
                .version(1)
                .timestamp(15276799200000000L)
                .random(UUID.fromString("5ef18239-8e6c-4e19-b222-c87fd06b003d"))
                .tag("String tag", Variant.ofString("String value"))
                .tag("Null tag", Variant.ofNull())
                .tag("Uuid tag", Variant.ofUuid(UUID.fromString("5ef18239-8e6c-4e19-b222-c87fd06b003d")))
                .tag("Container array tag", Variant.ofVector(Vector.ofContainers(
                        ContainerBuilder.create()
                                .tag("Some integer tag", Variant.ofInteger(123345567))
                                .build(),
                        ContainerBuilder.create()
                                .tag(
                                        "Byte vector tag",
                                        Variant.ofVector(Vector.ofBytes((byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF)))
                                .build())))
                .build();

        assertEquals(
                "One line event print is invalid",
                TestUtil.getResourceAsString("event-print-one-line.txt").trim(),
                EventFormatter.format(event, false)
        );

        assertEquals(
                "Pretty event print is invalid",
                TestUtil.getResourceAsString("event-print-pretty.txt").trim(),
                EventFormatter.format(event, true)
        );
    }


    @Test
    public void shouldPrintSimpleContainer() throws Exception {
        final Container inner = ContainerBuilder.create()
                .tag("Inner integer", Variant.ofInteger(123456))
                .tag("Inner string", Variant.ofString("Some inner value"))
                .build();

        final Container container = ContainerBuilder.create()
                .tag("Byte tag", Variant.ofByte((byte) 123))
                .tag("Short tag", Variant.ofShort((short) 12345))
                .tag("Integer tag", Variant.ofInteger(123456789))
                .tag("Long tag", Variant.ofLong(123456789123L))
                .tag("Float tag", Variant.ofFloat(123.456f))
                .tag("Double tag", Variant.ofDouble(123.456789))
                .tag("Flag tag", Variant.ofFlag(true))
                .tag("String tag", Variant.ofString("Some string"))
                .tag("Container tag", Variant.ofContainer(inner))
                .tag("Array of integer tag", Variant.ofVector(Vector.ofIntegers(1, 2, 3)))
                .tag("Array of container tag", Variant.ofVector(Vector.ofContainers(
                        ContainerBuilder.create()
                                .tag("String tag in array", Variant.ofString("Some string"))
                                .build(),
                        ContainerBuilder.create()
                                .tag("Flag array tag in array", Variant.ofVector(Vector.ofFlags(true, true, false)))
                                .build())))
                .build();

        assertEquals(
                "One line container print is invalid",
                TestUtil.getResourceAsString("container-print-one-line.txt").trim(),
                EventFormatter.format(container,  false));

        assertEquals(
                "Pretty container print is invalid",
                TestUtil.getResourceAsString("container-print-pretty.txt").trim(),
                EventFormatter.format(container,  true));
    }

}
