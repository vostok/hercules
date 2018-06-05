package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.ShardReadState;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;

import java.util.Iterator;

public class ShardReadStateReader implements Iterator<ShardReadState> {

    private final byte[] data;
    private final Decoder decoder;
    private int remaining;

    public ShardReadStateReader(byte[] data) {
        this.data = data;
        this.decoder = new Decoder(data);
        if(0 == data.length) {
            this.remaining = 0;
        } else if (data.length < 4) {
            throw new InvalidDataException();
        } else {
            this.remaining = decoder.readInteger();
            if (remaining < 0) {
                throw new InvalidDataException();
            }
        }
    }

    @Override
    public boolean hasNext() {
        return remaining != 0;
    }

    @Override
    public ShardReadState next() {
        --remaining;
        return new ShardReadState(
                decoder.readInteger(),
                decoder.readLong()
        );
    }
}
