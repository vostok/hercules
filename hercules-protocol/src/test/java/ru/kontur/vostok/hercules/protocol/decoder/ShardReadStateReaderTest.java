package ru.kontur.vostok.hercules.protocol.decoder;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.ShardReadState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class ShardReadStateReaderTest {

    @Test
    public void shouldDeserializeData() {
        byte[] data = {
                0x00, 0x00, 0x00, 0x02, // Partition count = 2
                0x00, 0x00, 0x00, 0x01, // Partition number = 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, // Partition 1 offset = 16
                0x00, 0x00, 0x00, 0x02, // Partition number = 2
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, // Partition 1 offset = 32
        };

        List<ShardReadState> states = new ArrayList<>();
        new ShardReadStateReader(data).forEachRemaining(states::add);

        assertEquals(2, states.size());
        assertEquals(states.get(0).getPartition(), 1);
        assertEquals(states.get(0).getOffset(), 16);
        assertEquals(states.get(1).getPartition(), 2);
        assertEquals(states.get(1).getOffset(), 32);
    }
}
