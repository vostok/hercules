package ru.kontur.vostok.hercules.protocol.decoder;

/**
 * Hercules Protocol Reader for vector
 * @param <T> type of vector
 * @author jdk
 */
public class VectorReader<T> extends CollectionReader<T> {
    public VectorReader(Reader<T> elementReader, Class<T> clazz) {
        super(elementReader, clazz);
    }

    @Override
    protected int readLength(Decoder decoder) {
        return decoder.readVectorLength();
    }
}
