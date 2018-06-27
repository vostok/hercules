package ru.kontur.vostok.hercules.protocol.encoder;

public interface Writer<T> {

    void write(Encoder encoder, T value);
}
