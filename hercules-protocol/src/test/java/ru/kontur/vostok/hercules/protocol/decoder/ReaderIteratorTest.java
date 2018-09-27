package ru.kontur.vostok.hercules.protocol.decoder;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class ReaderIteratorTest {

    @Test
    public void shouldReadWriteIntegerArray() throws InvalidDataException {
        ArrayWriter<Integer> writer = new ArrayWriter<>(Encoder::writeInteger);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        writer.write(encoder, new Integer[]{1, 2, 3});

        Decoder decoder = new Decoder(stream.toByteArray());
        ReaderIterator<Integer> reader = new ReaderIterator<>(decoder, Decoder::readInteger);

        List<Integer> result = new ArrayList<>();
        while (reader.hasNext()) {
            result.add(reader.next());
        }

        assertArrayEquals(new Integer[]{1, 2, 3}, result.toArray(new Integer[0]));
    }
}
