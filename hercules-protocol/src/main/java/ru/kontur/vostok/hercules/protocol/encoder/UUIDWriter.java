package ru.kontur.vostok.hercules.protocol.encoder;

import java.util.UUID;

public class UUIDWriter implements Writer<UUID> {

    @Override
    public void write(Encoder encoder, UUID value) {
        encoder.writeLong(value.getMostSignificantBits());
        encoder.writeLong(value.getLeastSignificantBits());
    }
}
