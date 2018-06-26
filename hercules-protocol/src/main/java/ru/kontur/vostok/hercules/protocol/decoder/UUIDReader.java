package ru.kontur.vostok.hercules.protocol.decoder;

import java.util.UUID;

public class UUIDReader implements Reader<UUID> {

    @Override
    public UUID read(Decoder decoder) {
        return new UUID(decoder.readLong(), decoder.readLong());
    }
}
