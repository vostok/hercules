package ru.kontur.vostok.hercules.protocol;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.VariantReader;
import ru.kontur.vostok.hercules.protocol.encoder.VariantWriter;

import java.util.Collections;

import static ru.kontur.vostok.hercules.protocol.TestUtil.multiply;

public class VariantWriteReadTest {

    private WriteReadPipe<Variant> pipe = WriteReadPipe.init(new VariantWriter(), new VariantReader());

    @Test
    public void shouldReadWriteContainer() throws Exception {
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
    public void shouldWriteReadText() {
        Variant variant = Variant.ofText("Abc def Абв гдё");

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadContainerVector() {
        Variant variant = Variant.ofContainerVector(new Container[]{
                new Container(Collections.singletonMap("first", Variant.ofInteger(1))),
                new Container(Collections.singletonMap("second", Variant.ofString("second"))),
                new Container(Collections.singletonMap("third", Variant.ofDoubleArray(new double[]{1.25, 1.3})))
        });

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadByteVector() {
        Variant variant = Variant.ofByteVector(new byte[]{1, 2, 3});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadShortVector() {
        Variant variant = Variant.ofShortVector(new short[]{10_000, 20_000, 30_000});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadIntegerVector() {
        Variant variant = Variant.ofIntegerVector(new int[]{1, 2, 123_456_789});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadLongVector() {
        Variant variant = Variant.ofLongVector(new long[]{1, 2, 123_456_789L});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFloatVector() {
        Variant variant = Variant.ofFloatVector(new float[]{1.23f, 4.56f, 7.89f});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadDoubleVector() {
        Variant variant = Variant.ofDoubleVector(new double[]{1.23, 4.56, 7.89});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFlagVector() {
        Variant variant = Variant.ofFlagVector(new boolean[]{true, true, false});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadStringVector() {
        Variant variant = Variant.ofStringVector(new String[]{"S", "F", "Ё"});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }


    @Test
    public void shouldWriteReadTextVector() {
        Variant variant = Variant.ofTextVector(new String[]{"S", "F", multiply("ЁЙЯ", 100)});

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadContainerArray() {
        Variant variant = Variant.ofContainerArray(multiply(new Container[]{
                new Container(Collections.singletonMap("first", Variant.ofInteger(1))),
                new Container(Collections.singletonMap("second", Variant.ofString("second"))),
                new Container(Collections.singletonMap("third", Variant.ofDoubleArray(new double[]{1.25, 1.3})))
        }, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadByteArray() {
        Variant variant = Variant.ofByteArray(multiply(new byte[]{1, 2, 3}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadShortArray() {
        Variant variant = Variant.ofShortArray(multiply(new short[]{10_000, 20_000, 30_000}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadIntegerArray() {
        Variant variant = Variant.ofIntegerArray(multiply(new int[]{1, 2, 123_456_789}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadLongArray() {
        Variant variant = Variant.ofLongArray(multiply(new long[]{1, 2, 123_456_789L}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFloatArray() {
        Variant variant = Variant.ofFloatArray(multiply(new float[]{1.23f, 4.56f, 7.89f}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadDoubleArray() {
        Variant variant = Variant.ofDoubleArray(multiply(new double[]{1.23, 4.56, 7.89}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadFlagArray() {
        Variant variant = Variant.ofFlagArray(multiply(new boolean[]{true, true, false}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }

    @Test
    public void shouldWriteReadStringArray() {
        Variant variant = Variant.ofStringArray(multiply(new String[]{"S", "F", "Ё"}, 100));

        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }


    @Test
    public void shouldWriteReadTextArray() {
        Variant variant = Variant.ofTextArray(multiply(new String[]{"S", "F", multiply("ЁЙЯ", 99)}, 100));
        pipe.process(variant).assertEquals(HerculesProtocolAssert::assertEquals);
    }
}
