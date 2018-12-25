package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.VariantReader;
import ru.kontur.vostok.hercules.protocol.encoder.VariantWriter;

import java.util.Collections;
import java.util.UUID;

import static ru.kontur.vostok.hercules.protocol.TestUtil.multiply;

public class VariantWriteReadTest {

    private WriteReadPipe<Variant> pipe = WriteReadPipe.init(new VariantWriter(), new VariantReader());

    @Test
    public void shouldWriteReadContainer() {
        Variant variant = Variant.ofContainer(new Container(
                Collections.singletonMap("value", Variant.ofInteger(123))
        ));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadByte() {
        Variant variant = Variant.ofByte((byte) 127);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadShort() {
        Variant variant = Variant.ofShort((short) 10_000);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadInteger() {
        Variant variant = Variant.ofInteger(123_456_789);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadLong() {
        Variant variant = Variant.ofLong(123_456_789L);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFloat() {
        Variant variant = Variant.ofFloat(0.123456f);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadDouble() {
        Variant variant = Variant.ofDouble(0.123456789);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFlag() {
        Variant variant = Variant.ofFlag(true);

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadString() {
        Variant variant = Variant.ofString("Abc def Абв гдё");

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadUuid() {
        Variant variant = Variant.ofUuid(UUID.fromString("11203800-63FD-11E8-83E2-3A587D902000"));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadNull() {
        Variant variant = Variant.ofNull();

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadContainerVector() {
        Variant variant = Variant.ofVector(Vector.ofContainers(
                new Container(Collections.singletonMap("first", Variant.ofInteger(1))),
                new Container(Collections.singletonMap("second", Variant.ofString("second"))),
                new Container(Collections.singletonMap("third", Variant.ofVector(Vector.ofDoubles(1.25, 1.3))))));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadByteVector() {
        Variant variant = Variant.ofVector(Vector.ofBytes(new byte[]{1, 2, 3}));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadShortVector() {
        Variant variant = Variant.ofVector(Vector.ofShorts(new short[]{10_000, 20_000, 30_000}));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadIntegerVector() {
        Variant variant = Variant.ofVector(Vector.ofIntegers(1, 2, 123_456_789));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadLongVector() {
        Variant variant = Variant.ofVector(Vector.ofLongs(1, 2, 123_456_789L));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFloatVector() {
        Variant variant = Variant.ofVector(Vector.ofFloats(1.23f, 4.56f, 7.89f));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadDoubleVector() {
        Variant variant = Variant.ofVector(Vector.ofDoubles(1.23, 4.56, 7.89));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFlagVector() {
        Variant variant = Variant.ofVector(Vector.ofFlags(true, true, false));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadStringVector() {
        Variant variant = Variant.ofVector(Vector.ofStrings("S", "F", "Ё"));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadUuidVector() {
        Variant variant = Variant.ofVector(Vector.ofUuids(UUID.randomUUID(), UUID.randomUUID()));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadNullVector() {
        Variant variant = Variant.ofVector(Vector.ofNulls(null, null));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
