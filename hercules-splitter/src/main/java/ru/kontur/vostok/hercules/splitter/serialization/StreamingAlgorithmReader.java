package ru.kontur.vostok.hercules.splitter.serialization;

import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.splitter.service.StreamingAlgorithm;

/**
 * {@link Reader} implementation that delegates work to given {@link StreamingAlgorithm}.
 *
 * @param <T> Type of object this reader should create after reading.
 * @author Aleksandr Yuferov
 */
public class StreamingAlgorithmReader<T> implements Reader<T> {

    private final Reader<T> originalReader;
    private final T returnValue;
    private final StreamingAlgorithm algorithm;

    /**
     * Constructor.
     *
     * @param originalReader Original reader of objects of type {@link T}.
     * @param returnValue    Value should be returned by {@link #read} method.
     * @param algorithm      {@link StreamingAlgorithm} that will perform parsing in {@link #read} method.
     */
    public StreamingAlgorithmReader(Reader<T> originalReader, T returnValue, StreamingAlgorithm algorithm) {
        this.originalReader = originalReader;
        this.returnValue = returnValue;
        this.algorithm = algorithm;
    }

    @Override
    public T read(Decoder decoder) {
        int begin = decoder.position();
        int length = originalReader.skip(decoder);
        algorithm.update(decoder.array(), begin, length);
        return returnValue;
    }

    @Override
    public int skip(Decoder decoder) {
        return originalReader.skip(decoder);
    }
}
