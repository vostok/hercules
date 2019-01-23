package ru.kontur.vostok.hercules.kafka.util.serialization;

/**
 * DeserializationException
 *
 * @author Kirill Sulim
 */
public class DeserializationException {

    private final byte[] bytes;
    private final Exception cause;

    public DeserializationException(byte[] bytes, Exception cause) {
        this.bytes = bytes;
        this.cause = cause;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Exception getCause() {
        return cause;
    }
}
