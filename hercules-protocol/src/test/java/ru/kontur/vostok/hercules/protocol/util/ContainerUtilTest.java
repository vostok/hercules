package ru.kontur.vostok.hercules.protocol.util;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TestUtil;
import ru.kontur.vostok.hercules.protocol.Variant;

import static org.junit.Assert.*;

public class ContainerUtilTest {

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
                .tag("Array of integer tag", Variant.ofIntegerArray(new int[]{1, 2, 3}))
                .tag("Array of container tag", Variant.ofContainerArray(new Container[]{
                        ContainerBuilder.create()
                            .tag("String tag in array", Variant.ofString("Some string"))
                            .build(),
                        ContainerBuilder.create()
                            .tag("Flag array tag in array", Variant.ofFlagArray(new boolean[] {true, true, false}))
                            .build()
                }))
                .build();

        assertEquals(
                "One line container print is invalid",
                TestUtil.getResourceAsString("container-print-one-line.txt"),
                ContainerUtil.toString(container,  false) + "\n"
        );

        assertEquals(
                "Pretty container print is invalid",
                TestUtil.getResourceAsString("container-print-pretty.txt"),
                ContainerUtil.toString(container,  true) + "\n"
        );
    }

}
