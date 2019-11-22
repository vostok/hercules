package ru.kontur.vostok.hercules.protocol;

import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.Writer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class WriteReadPipe<T> {

    public static class ProcessedCapture<T> {

        private final T original;
        private final T processed;

        public ProcessedCapture(T original, T processed) {
            this.original = original;
            this.processed = processed;
        }

        public T getOriginal() {
            return original;
        }

        public T getProcessed() {
            return processed;
        }

        public void assertEquals(BiConsumer<T, T> asserter) {
            asserter.accept(original, processed);
        }
    }

    private final Writer<T> writer;
    private final Reader<T> reader;

    private WriteReadPipe(Writer<T> writer, Reader<T> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    public ProcessedCapture<T> process(T original) {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        Encoder encoder = new Encoder(buffer);
        writer.write(encoder, original);

        buffer.flip();
        Decoder decoder = new Decoder(buffer);
        T processed = reader.read(decoder);

        return new ProcessedCapture<>(original, processed);
    }

    public static <T> WriteReadPipe<T> init(Writer<T> writer, Reader<T> reader) {
        return new WriteReadPipe<>(writer, reader);
    }
}
