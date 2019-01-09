package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TestUtil;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.UUID;

import static org.junit.Assert.*;

public class EventUtilTest {

    @Test
    public void shouldPrettyPrintEvent() throws Exception {
        final Event event = EventBuilder.create()
                .setVersion(1)
                .setEventId(UUID.fromString("5ef18239-8e6c-4e19-b222-c87fd06b003d"))
                .setTag("String tag", Variant.ofString("String value"))
                .setTag("Container array tag", Variant.ofContainerArray(new Container[]{
                        ContainerBuilder.create()
                            .tag("Some integer tag", Variant.ofInteger(123345567))
                            .build(),
                        ContainerBuilder.create()
                            .tag(
                                    "Byte vector tag",
                                    Variant.ofByteVector(new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF})
                            )
                            .build()
                }))
                .build();

        assertEquals(
                "One line event print is invalid",
                TestUtil.getResourceAsString("event-print-one-line.txt"),
                EventUtil.toString(event, false) + "\n"
        );

        assertEquals(
                "Pretty event print is invalid",
                TestUtil.getResourceAsString("event-print-pretty.txt"),
                EventUtil.toString(event, true) + "\n"
        );
    }
}
