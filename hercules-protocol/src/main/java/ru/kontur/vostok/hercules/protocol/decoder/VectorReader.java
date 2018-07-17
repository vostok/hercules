package ru.kontur.vostok.hercules.protocol.decoder;

/**
 * Hercules Protocol Reader for vector
 *
 * @param <T> Type of collection for which defined Reader<T>
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
