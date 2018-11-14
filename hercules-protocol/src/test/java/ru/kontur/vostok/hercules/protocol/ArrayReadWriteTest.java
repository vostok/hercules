package ru.kontur.vostok.hercules.protocol;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.ArrayReader;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.util.Objects;

import static org.junit.Assert.assertArrayEquals;

public class ArrayReadWriteTest {

    private static class Demo {
        private int i;
        private boolean f;

        public Demo(int i, boolean f) {
            this.i = i;
            this.f = f;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Demo demo = (Demo) o;
            return i == demo.i &&
                    f == demo.f;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, f);
        }
    }

    @Test
    public void shouldReadWriteArrayOfIntegers() {
        WriteReadPipe
                .init(new ArrayWriter<>(Encoder::writeInteger), new ArrayReader<>(Decoder::readInteger, Integer.class))
                .process(new Integer[]{123, 456, 789})
                .assertEquals(Assert::assertArrayEquals);
    }

    @Test
    public void shouldReadWriteDemoArray() {
        ArrayWriter<Demo> demoArrayWriter = new ArrayWriter<>((encoder, demo) -> {
            encoder.writeInteger(demo.i);
            encoder.writeFlag(demo.f);
        });
        ArrayReader<Demo> demoArrayReader = new ArrayReader<>(
                decoder -> new Demo(decoder.readInteger(), decoder.readFlag()),
                Demo.class
        );
        WriteReadPipe
                .init(demoArrayWriter, demoArrayReader)
                .process(new Demo[]{new Demo(1, true), new Demo(2, false)})
                .assertEquals(Assert::assertArrayEquals);
    }
}
